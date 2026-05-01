package com.contractiq.service;

import com.contractiq.domain.notification.NotificationLog;
import com.contractiq.dto.notification.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationRetryService {

    private final NotificationLogService notificationLogService;
    private final NotificationService notificationService;

    @Value("${app.notifications.retry.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.notifications.retry.retry-delay-ms:60000}")
    private long retryDelayMs;

    @Value("${app.notifications.retry.batch-size:10}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.notifications.retry.fixed-delay-ms:30000}")
    public void retryFailedNotifications() {
        for (int processed = 0; processed < batchSize; processed++) {
            Optional<NotificationLog> claimedLog = notificationLogService.claimNextRetry(maxAttempts);
            if (claimedLog.isEmpty()) {
                return;
            }

            NotificationLog logEntry = claimedLog.get();
            try {
                notificationService.send(
                        NotificationMessage.builder()
                                .toEmail(logEntry.getToEmail())
                                .subject(logEntry.getType())
                                .body(logEntry.getMessage())
                                .build()
                );
                notificationLogService.markSent(logEntry.getEventId());
            } catch (RuntimeException ex) {
                notificationLogService.markFailed(logEntry.getEventId(), ex.getMessage(), maxAttempts, retryDelayMs);
                log.error("Notification retry failed for event {}", logEntry.getEventId(), ex);
            }
        }
    }
}
