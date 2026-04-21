package com.grid07.assignment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class NotificationService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void handleBotNotification(Long userId, String botName, Long postId) {
        String cooldownKey = "notif_cooldown:user_" + userId;
        String pendingKey = "user:" + userId + ":pending_notifs";

        String msg = "Bot " + botName + " replied to your post " + postId;

        Boolean hasCooldown = redisTemplate.hasKey(cooldownKey);

        if (Boolean.TRUE.equals(hasCooldown)) {
            redisTemplate.opsForList().rightPush(pendingKey, msg);
        } else {
            System.out.println("Push Notification Sent to User " + userId + ": " + msg);
            redisTemplate.opsForValue().set(cooldownKey, "1", 15, TimeUnit.MINUTES);
        }
    }
}
