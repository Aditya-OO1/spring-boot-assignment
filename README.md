## Thought Process

- I chose Redis for rate limiting because it provides atomic operations like INCR, which helps in handling concurrent requests without race conditions.
- Initially, I considered using the database for enforcing limits, but it could lead to performance issues and conflicts under high traffic.
- For notification batching, I used Redis lists to temporarily store events and process them every 5 minutes using a scheduled task.
- I focused on keeping the implementation simple and efficient while ensuring scalability for higher loads.

## Future Improvements

- Add retry mechanisms for failed operations
- Improve error handling and logging
- Introduce authentication and authorization
- Optimize batching strategy for large-scale systems
# Grid07 Backend Assignment

Spring Boot microservice with Redis-backed guardrails and a PostgreSQL source of truth.

## Running Locally

```bash
docker-compose up -d
mvn spring-boot:run
```

Postgres runs on 5432, Redis on 6379, app on 8080.

## Approach

### Phase 2 — Thread Safety for Atomic Locks

The core concurrency problem is the horizontal cap: 200 simultaneous bot requests must not push the count past 100.

I used Redis `INCR` for this. `INCR` is a single atomic operation on the Redis server — it reads and increments the value in one step with no race window between them. So even if 200 threads call it at the exact same millisecond, each one gets back a distinct integer (1, 2, 3 ... 200). The ones that get back a value > 100 immediately decrement and get rejected with 429. The first 100 go through.

This works because Redis is single-threaded internally — all commands are serialized, so there's no way two threads can both read 99 and both think they're the 100th.

The cooldown lock uses `SET key value EX 600` (via Spring's `redisTemplate.opsForValue().set(key, val, 10, MINUTES)`). Redis SET with TTL is also atomic, so there's no gap where two requests could both find the key missing and both set it.

**No Java-side synchronization is needed** — and no `HashMap`, `synchronized`, or `static` counters are used anywhere. All state lives in Redis.

### Phase 3 — Notification Batching

When a bot interacts, we check a per-user cooldown key in Redis. If it's set, the notification string gets pushed into a Redis List (`RPUSH`). If not, we log immediately and set the 15-minute cooldown.

A `@Scheduled` task runs every 5 minutes, scans all `user:*:pending_notifs` keys, pops everything off each list, and logs a single summarized line per user. The list is then deleted.

### Data Integrity

Redis guardrails are checked before any DB write. If Redis rejects the request (cap hit, cooldown active, depth exceeded), we throw early and the `@Transactional` method never reaches `repository.save()`, so no partial data ends up in Postgres.

## Project Structure

```
src/main/java/com/grid07/assignment/
├── config/         # Redis config, global exception handler
├── controller/     # REST endpoints
├── dto/            # Request bodies
├── entity/         # JPA entities
├── repository/     # Spring Data repos
└── service/        # Business logic, virality, notifications, scheduler
```
