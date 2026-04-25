package com.assignment.backend.scheduler;

import com.assignment.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSweeper {

    private final StringRedisTemplate redisTemplate;
    private final NotificationService notificationService;

    // runs every 5 minutes to batch up bot notifications. TODO: actually send
    // push/email someday for now just logging
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void sweepPendingNotifications() {
        log.info("running notification sweep");

        Set<String> pendingKeys = redisTemplate.keys("user:*:pending_notifs");

        if (pendingKeys == null || pendingKeys.isEmpty()) {
            log.info("No pending notifications found");
            return;
        }

        for (String key : pendingKeys) {
            try {
                String[] parts = key.split(":");
                if (parts.length < 2)
                    continue;

                Long userId = Long.parseLong(parts[1]);

                List<String> messages = notificationService.getAndClearPending(userId);

                if (messages != null && !messages.isEmpty()) {
                    String firstBot = extractBotName(messages.get(0));
                    int othersCount = messages.size() - 1;

                    String summary;
                    if (othersCount == 0) {
                        summary = String.format("Summarized Push Notification: %s interacted with your posts",
                                firstBot);
                    } else {
                        summary = String.format(
                                "Summarized Push Notification: %s and %d others interacted with your posts",
                                firstBot, othersCount);
                    }

                    log.info(summary);
                }

            } catch (Exception e) {
                log.error("Error processing notifications for key {}: {}", key, e.getMessage());
            }
        }

        log.info("sweep done");
    }

    private String extractBotName(String message) {
        if (message.startsWith("Bot ")) {
            String[] parts = message.split(" ");
            if (parts.length >= 2) {
                return "Bot " + parts[1];
            }
        }
        return "A bot";
    }
}
