package com.assignment.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ViralityService {

    private final StringRedisTemplate redisTemplate;

    private static final long BOT_REPLY_POINTS = 1;
    private static final long HUMAN_LIKE_POINTS = 20;
    private static final long HUMAN_COMMENT_POINTS = 50;

    public void incrementScore(Long postId, String interactionType) {
        String key = "post:" + postId + ":virality_score";

        long points = switch (interactionType) {
            case "BOT_REPLY" -> BOT_REPLY_POINTS;
            case "HUMAN_LIKE" -> HUMAN_LIKE_POINTS;
            case "HUMAN_COMMENT" -> HUMAN_COMMENT_POINTS;
            default -> 0;
        };

        redisTemplate.opsForValue().increment(key, points);
    }

    public Long getScore(Long postId) {
        String key = "post:" + postId + ":virality_score";
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }
}
