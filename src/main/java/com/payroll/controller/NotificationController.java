package com.payroll.controller;

import com.payroll.dto.ApiResponse;
import com.payroll.model.Notification;
import com.payroll.security.CustomUserDetails;
import com.payroll.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications() {
        CustomUserDetails user = getAuthenticatedUserDetails();
        List<Notification> list = notificationService.getUserNotifications(user.getEmployeeId(), user.getUser().getRole());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        CustomUserDetails user = getAuthenticatedUserDetails();
        long count = notificationService.getUnreadCount(user.getEmployeeId(), user.getUser().getRole());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("unreadCount", count)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.ok("Marked as read", null));
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        CustomUserDetails user = getAuthenticatedUserDetails();
        notificationService.markAllAsRead(user.getEmployeeId(), user.getUser().getRole());
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read", null));
    }

    private CustomUserDetails getAuthenticatedUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (CustomUserDetails) auth.getPrincipal();
    }
}
