package com.example.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(

        UUID id,

        UUID eventId,

        UUID orderId,

        String customerEmail,

        String message,

        LocalDateTime createdAt
) {
}
