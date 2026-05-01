package com.contractiq.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private String id;
    private String eventId;
    private String eventVersion;
    private String toEmail;
    private String type;
    private String contractId;
    private String message;
    private String status;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
