package com.grid07.assignment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class NotificationSweeper {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Scheduled(fixedRate = 300000)
    public void sweepPendingNotifications() {
        Set<String> keys = redisTemplate.keys("user:*:pending_notifs");

        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            Long size = redisTemplate.opsForList().size(key);
            if (size == null || size == 0) continue;

            List<String> messages = new ArrayList<>();
            String msg;
            while ((msg = redisTemplate.opsForList().leftPop(key)) != null) {
                messages.add(msg);
            }

            if (messages.isEmpty()) continue;

            String firstMsg = messages.get(0);
            String botName = extractBotName(firstMsg);
            int others = messages.size() - 1;

            if (others > 0) {
                System.out.println("Summarized Push Notification: " + botName + " and [" + others + "] others interacted with your posts.");
            } else {
                System.out.println("Summarized Push Notification: " + firstMsg);
            }

            redisTemplate.delete(key);
        }
    }

    private String extractBotName(String message) {
        try {
            return message.split(" ")[1];
        } catch (Exception e) {
            return "Unknown Bot";
        }
    }
}
