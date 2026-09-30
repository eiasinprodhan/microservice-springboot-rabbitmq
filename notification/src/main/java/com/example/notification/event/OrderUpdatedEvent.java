package com.example.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderUpdatedEvent(

        UUID eventId,

        UUID orderId,

        String productName,

        BigDecimal amount,

        String customerEmail,

        String previousStatus,

        String newStatus,

        LocalDateTime updatedAt
) {
}
