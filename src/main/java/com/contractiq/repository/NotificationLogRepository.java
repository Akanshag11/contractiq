package com.contractiq.repository;

import com.contractiq.domain.notification.NotificationLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationLogRepository extends MongoRepository<NotificationLog, String> {
    List<NotificationLog> findByToEmailOrderByCreatedAtDesc(String toEmail);
    long countByToEmailAndReadFalse(String toEmail);
    Optional<NotificationLog> findByEventId(String eventId);
}
