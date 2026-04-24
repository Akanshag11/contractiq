package com.contractiq.kafka;

import com.contractiq.dto.notification.NotificationMessage;
import com.contractiq.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContractEventConsumer {
    private final NotificationService notificationService;

    @KafkaListener(topics = "contract-events", groupId = "contract-events-group")
    public void consume(ContractEventMessage event) {
        notificationService.send(NotificationMessage.builder()
                .toEmail(event.getToEmail())
                .subject(event.getType())
                .body(event.getMessage()).build());
    }
}
