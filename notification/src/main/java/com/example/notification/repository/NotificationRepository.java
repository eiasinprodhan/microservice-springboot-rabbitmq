package com.example.notification.repository;

import com.example.notification.entity.Notification;
import com.example.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByEventId(UUID eventId);

    List<Notification> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<Notification> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String customerEmail);

    List<Notification> findByCustomerEmailIgnoreCaseAndIsReadFalse(String customerEmail);

    Page<Notification> findByCustomerEmailIgnoreCase(String customerEmail, Pageable pageable);

    Page<Notification> findByIsRead(boolean isRead, Pageable pageable);

    Page<Notification> findByType(NotificationType type, Pageable pageable);

    long countByIsRead(boolean isRead);

    long countByType(NotificationType type);
}
