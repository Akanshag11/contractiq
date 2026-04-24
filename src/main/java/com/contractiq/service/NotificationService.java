package com.contractiq.service;

import com.contractiq.dto.notification.NotificationMessage;

public interface NotificationService {
    void send(NotificationMessage message);
}
