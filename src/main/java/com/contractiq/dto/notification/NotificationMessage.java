package com.contractiq.dto.notification;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class NotificationMessage {
    private String toEmail;
    private String subject;
    private String body;
}
