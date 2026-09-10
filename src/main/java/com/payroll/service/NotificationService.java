package com.payroll.service;

import com.payroll.model.Notification;
import com.payroll.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getUserNotifications(Long employeeId, String role) {
        return notificationRepository.findForUser(employeeId, role);
    }

    public long getUnreadCount(Long employeeId, String role) {
        return notificationRepository.countUnreadForUser(employeeId, role);
    }

    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(Long employeeId, String role) {
        List<Notification> list = notificationRepository.findForUser(employeeId, role);
        for (Notification n : list) {
            if (!n.isRead()) {
                n.setRead(true);
            }
        }
        notificationRepository.saveAll(list);
    }

    @Transactional
    public Notification sendNotification(Long employeeId, String role, String title, String message, String type) {
        Notification n = new Notification(employeeId, role, title, message, type);
        return notificationRepository.save(n);
    }
}
