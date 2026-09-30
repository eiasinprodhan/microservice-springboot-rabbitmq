package com.example.notification.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationProcessedEvent(

        UUID eventId,

        UUID orderId,

        UUID notificationId,

        String status,

        LocalDateTime processedAt
) {
}
