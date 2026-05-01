package com.contractiq.service;

import com.contractiq.domain.notification.NotificationLog;
import com.contractiq.dto.response.NotificationResponse;
import com.contractiq.kafka.ContractEventMessage;
import com.contractiq.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationLogService {

    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_DEAD = "DEAD";

    private final NotificationLogRepository notificationLogRepository;
    private final MongoTemplate mongoTemplate;

    public boolean reserveEvent(ContractEventMessage event) {
        LocalDateTime now = LocalDateTime.now();
        NotificationLog log = NotificationLog.builder()
                .eventId(event.getEventId())
                .eventVersion(event.getEventVersion())
                .toEmail(event.getToEmail())
                .type(event.getType())
                .contractId(event.getContractId())
                .message(event.getMessage())
                .status(STATUS_PROCESSING)
                .attemptCount(1)
                .lastAttemptAt(now)
                .createdAt(now)
                .build();

        try {
            notificationLogRepository.save(log);
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    public void markSent(String eventId) {
        NotificationLog log = notificationLogRepository.findByEventId(eventId)
                .orElseThrow(() -> new RuntimeException("Notification event not found"));
        log.setStatus(STATUS_SENT);
        log.setFailureReason(null);
        log.setNextRetryAt(null);
        notificationLogRepository.save(log);
    }

    public void markFailed(String eventId, String failureReason, int maxAttempts, long retryDelayMs) {
        NotificationLog log = notificationLogRepository.findByEventId(eventId)
                .orElseThrow(() -> new RuntimeException("Notification event not found"));

        LocalDateTime now = LocalDateTime.now();
        log.setLastAttemptAt(now);
        log.setFailureReason(normalizeFailureReason(failureReason));

        if (log.getAttemptCount() >= maxAttempts) {
            log.setStatus(STATUS_DEAD);
            log.setNextRetryAt(null);
        } else {
            log.setStatus(STATUS_FAILED);
            log.setNextRetryAt(now.plus(Duration.ofMillis(retryDelayMs)));
        }

        notificationLogRepository.save(log);
    }

    public Optional<NotificationLog> claimNextRetry(int maxAttempts) {
        LocalDateTime now = LocalDateTime.now();

        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(STATUS_FAILED)
                .and("attemptCount").lt(maxAttempts)
                .and("nextRetryAt").lte(now));
        query.with(Sort.by(Sort.Direction.ASC, "nextRetryAt", "createdAt"));

        Update update = new Update()
                .set("status", STATUS_PROCESSING)
                .set("lastAttemptAt", now)
                .set("failureReason", null)
                .set("nextRetryAt", null)
                .inc("attemptCount", 1);

        NotificationLog claimed = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                NotificationLog.class
        );

        return Optional.ofNullable(claimed);
    }

    public List<NotificationResponse> getMyNotifications(Authentication authentication) {
        return notificationLogRepository.findByToEmailOrderByCreatedAtDesc(authentication.getName())
                .stream()
                .map(log -> NotificationResponse.builder()
                        .id(log.getId())
                        .eventId(log.getEventId())
                        .eventVersion(log.getEventVersion())
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
                .eventVersion(log.getEventVersion())
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

    private String normalizeFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return "Unknown delivery failure";
        }
        return failureReason;
    }
}
