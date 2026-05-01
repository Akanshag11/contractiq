package com.contractiq.domain.notification;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notification_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {
    @Id
    private String id;
    @Indexed(unique = true, sparse = true)
    private String eventId;
    private String eventVersion;
    private String toEmail;
    private String type;
    private String contractId;
    private String message;
    private String status;
    @Builder.Default
    private int attemptCount = 0;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime nextRetryAt;
    private String failureReason;
    @Builder.Default
    private boolean read = false;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
