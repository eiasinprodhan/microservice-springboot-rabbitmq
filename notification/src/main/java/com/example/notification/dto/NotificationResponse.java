package com.example.notification.dto;

import com.example.notification.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(

        UUID id,

        UUID eventId,

        UUID orderId,

        String customerEmail,

        NotificationType type,

        String message,

        boolean isRead,

        LocalDateTime readAt,

        LocalDateTime createdAt
) {
}
