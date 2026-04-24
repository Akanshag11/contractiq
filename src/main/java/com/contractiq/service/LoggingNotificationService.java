package com.contractiq.service;

import com.contractiq.dto.notification.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoggingNotificationService implements NotificationService{

    @Override
    public void send(NotificationMessage message) {
         log.info("Notification sent To : {},  Subject : {} , Body: {} ", message.getToEmail(),message.getSubject(),message.getBody());
    }
}
