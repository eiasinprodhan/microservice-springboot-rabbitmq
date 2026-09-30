package com.example.notification.controller;

import com.example.notification.dto.NotificationResponse;
import com.example.notification.entity.Notification;
import com.example.notification.exception.ResourceNotFoundException;
import com.example.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    // Get all notifications
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications() {

        List<NotificationResponse> notifications =
                notificationRepository.findAll()
                        .stream()
                        .map(this::mapToResponse)
                        .toList();

        return ResponseEntity.ok(notifications);
    }

    // Get one notification
    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotification(
            @PathVariable UUID id
    ) {

        Notification notification =
                notificationRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Notification not found with id: " + id
                                )
                        );

        return ResponseEntity.ok(
                mapToResponse(notification)
        );
    }

    private NotificationResponse mapToResponse(
            Notification notification
    ) {

        return new NotificationResponse(
                notification.getId(),
                notification.getEventId(),
                notification.getOrderId(),
                notification.getCustomerEmail(),
                notification.getMessage(),
                notification.getCreatedAt()
        );
    }
}
