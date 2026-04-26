package com.contractiq.service;

import com.contractiq.domain.notification.NotificationLog;
import com.contractiq.dto.response.NotificationResponse;
import com.contractiq.kafka.ContractEventMessage;
import com.contractiq.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationLogService {

    private final NotificationLogRepository notificationLogRepository;

    public void saveFromEvent(ContractEventMessage event, String status)
    {
        NotificationLog log=NotificationLog.builder()
                .eventId(event.getEventId())
                .toEmail(event.getToEmail())
                .type(event.getType())
                .contractId(event.getContractId())
                .message(event.getMessage())
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();

        notificationLogRepository.save(log);
    }

    public List<NotificationResponse> getMyNotifications(Authentication authentication) {
        return notificationLogRepository.findByToEmailOrderByCreatedAtDesc(authentication.getName())
                .stream()
                .map(log -> NotificationResponse.builder()
                        .id(log.getId())
                        .eventId(log.getEventId())
                        .toEmail(log.getToEmail())
                        .type(log.getType())
                        .contractId(log.getContractId())
                        .message(log.getMessage())
                        .status(log.getStatus())
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();
    }

    public long getUnreadCount(Authentication authentication) {
        return notificationLogRepository.countByToEmailAndReadFalse(authentication.getName());
    }

    public NotificationResponse markAsRead(String id, Authentication authentication) {
        NotificationLog log=notificationLogRepository.findById(id).orElseThrow(() -> new RuntimeException("Notification not found"));

        if(!log.getToEmail().equals(authentication.getName()))
        {
            throw new RuntimeException("You are not authorized to mark this notification as read");
        }
        log.setRead(true);
        log.setReadAt(LocalDateTime.now());

        NotificationLog saved = notificationLogRepository.save(log);

        return NotificationResponse.builder()
                .id(saved.getId())
                .eventId(log.getEventId())
                .toEmail(saved.getToEmail())
                .type(saved.getType())
                .contractId(saved.getContractId())
                .message(saved.getMessage())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .read(saved.isRead())
                .readAt(saved.getReadAt())
                .build();
    }
    public boolean isAlreadyProcessed(String eventId) {
        return notificationLogRepository.existsByEventId(eventId);
    }
}
