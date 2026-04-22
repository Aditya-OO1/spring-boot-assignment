package com.grid07.assignment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ViralityService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void addBotReply(Long postId) {
        String key = "post:" + postId + ":virality_score";
        redisTemplate.opsForValue().increment(key, 1);
    }

    public void addHumanLike(Long postId) {
        String key = "post:" + postId + ":virality_score";
        redisTemplate.opsForValue().increment(key, 20);
    }

    public void addHumanComment(Long postId) {
        String key = "post:" + postId + ":virality_score";
        redisTemplate.opsForValue().increment(key, 50);
    }

    public Long getViralityScore(Long postId) {
        String key = "post:" + postId + ":virality_score";
        String val = redisTemplate.opsForValue().get(key);
        if (val == null) return 0L;
        return Long.parseLong(val);
    }

    public boolean checkAndIncrementBotCount(Long postId) {
        String key = "post:" + postId + ":bot_count";
        Long count = redisTemplate.opsForValue().increment(key, 1);
        if (count > 100) {
            redisTemplate.opsForValue().decrement(key);
            return false;
        }
        return true;
    }

    public boolean isCooldownActive(Long botId, Long humanId) {
        String key = "cooldown:bot_" + botId + ":human_" + humanId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void setCooldown(Long botId, Long humanId) {
        String key = "cooldown:bot_" + botId + ":human_" + humanId;
        redisTemplate.opsForValue().set(key, "1", 10, TimeUnit.MINUTES);
    }
}

// TODO: Need to check why validation is slow here
// System.out.println("Debugging user flow");
