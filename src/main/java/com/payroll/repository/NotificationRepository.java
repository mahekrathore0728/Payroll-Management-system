package com.payroll.repository;

import com.payroll.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    @Query("SELECT n FROM Notification n WHERE " +
           "(n.recipientEmployeeId = :empId OR (n.recipientEmployeeId IS NULL AND (n.recipientRole IS NULL OR n.recipientRole = :role))) " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findForUser(@Param("empId") Long empId, @Param("role") String role);

    @Query("SELECT COUNT(n) FROM Notification n WHERE " +
           "(n.recipientEmployeeId = :empId OR (n.recipientEmployeeId IS NULL AND (n.recipientRole IS NULL OR n.recipientRole = :role))) " +
           "AND n.isRead = false")
    long countUnreadForUser(@Param("empId") Long empId, @Param("role") String role);
}
