package com.assignment.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class GuardrailService {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_BOT_COMMENTS = 100;
    private static final int MAX_DEPTH_LEVEL = 20;
    private static final long COOLDOWN_MINUTES = 10;

    // cap is 100 bots per post. increment first then check this way redis handles
    // race conditions atomically
    public boolean checkAndIncrementBotCount(Long postId) {
        String key = "post:" + postId + ":bot_count";

        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount > MAX_BOT_COMMENTS) {
            redisTemplate.opsForValue().decrement(key); // rollback since we went over
            return false;
        }

        redisTemplate.expire(key, 24, TimeUnit.HOURS);
        return true;
    }

    // just checking the depth
    public boolean checkDepthLevel(int depthLevel) {
        return depthLevel <= MAX_DEPTH_LEVEL;
    }

    // setnx with ttl if key already exists means cooldown is still active
    public boolean checkCooldown(Long botId, Long humanId) {
        String key = "cooldown:bot_" + botId + ":human_" + humanId;

        Boolean setSuccess = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", COOLDOWN_MINUTES, TimeUnit.MINUTES);

        return setSuccess != null && setSuccess;
    }
}
