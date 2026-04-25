package com.assignment.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final StringRedisTemplate redisTemplate;

    private static final long NOTIFICATION_COOLDOWN_MINUTES = 15;

    public void handleBotInteraction(Long userId, String message) {
        String cooldownKey = "notification_cooldown:user_" + userId;
        String pendingKey = "user:" + userId + ":pending_notifs";

        Boolean hasCooldown = redisTemplate.hasKey(cooldownKey);

        if (Boolean.TRUE.equals(hasCooldown)) {
            redisTemplate.opsForList().rightPush(pendingKey, message);
            log.info("Notification queued for user {}: {}", userId, message);
        } else {
            log.info("Push Notification Sent to User {}: {}", userId, message);
            redisTemplate.opsForValue().set(cooldownKey, "1",
                    NOTIFICATION_COOLDOWN_MINUTES, TimeUnit.MINUTES);
        }
    }

    public List<String> getAndClearPending(Long userId) {
        String pendingKey = "user:" + userId + ":pending_notifs";

        List<String> messages = redisTemplate.opsForList().range(pendingKey, 0, -1);
        redisTemplate.delete(pendingKey);

        return messages;
    }
}
