package com.contractiq.kafka;

import com.contractiq.dto.notification.NotificationMessage;
import com.contractiq.service.NotificationLogService;
import com.contractiq.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractEventConsumer {
    private final NotificationService notificationService;
    private final NotificationLogService notificationLogService;

    @Value("${app.notifications.retry.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.notifications.retry.retry-delay-ms:60000}")
    private long retryDelayMs;

    @KafkaListener(topics = "contract-events", groupId = "contract-events-group")
    public void consume(ContractEventMessage event) {
       validateSupportedVersion(event);
        if (!notificationLogService.reserveEvent(event)) {
            return;
        }

        try {
            notificationService.send(
                    NotificationMessage.builder()
                            .toEmail(event.getToEmail())
                            .subject(event.getType())
                            .body(event.getMessage())
                            .build()
            );

            notificationLogService.markSent(event.getEventId());
        } catch (RuntimeException ex) {
            notificationLogService.markFailed(event.getEventId(), ex.getMessage(), maxAttempts, retryDelayMs);
            log.error("Notification delivery failed for event {}", event.getEventId(), ex);
        }
    }

    private void validateSupportedVersion(ContractEventMessage event) {
        String version = event.getEventVersion();

        if (version == null || version.isBlank()) {
            event.setEventVersion("v1");
            return;
        }

        if (!version.equals("v1")) {
            throw new RuntimeException("Unsupported event version: " + version);
        }
    }
}
