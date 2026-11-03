package com.hangout.controller;

import com.hangout.config.SecurityUtils;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Notification;
import com.hangout.repository.NotificationRepository;
import com.hangout.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<List<Notification>> getMine() {
        return ResponseEntity.ok(notificationService.getUserNotifications(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        long count = notificationService.getUnreadCount(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new AccessDeniedException("You cannot modify this notification");
        }
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
}
