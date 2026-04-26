package com.contractiq.controller;

import com.contractiq.dto.response.NotificationResponse;
import com.contractiq.service.NotificationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationLogService notificationLogService;

    @GetMapping("/my")
    public List<NotificationResponse> getMyNotifications(Authentication authentication) {
        return notificationLogService.getMyNotifications(authentication);
    }
    @GetMapping("/unread-count")
    public long getUnreadCount(Authentication authentication) {
        return notificationLogService.getUnreadCount(authentication);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable String id,
                                           Authentication authentication) {
        return notificationLogService.markAsRead(id, authentication);
    }
}
