package com.example.orders.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCancelledEvent(

        UUID eventId,

        UUID orderId,

        String productName,

        BigDecimal amount,

        String customerEmail,

        String cancellationReason,

        LocalDateTime cancelledAt
) {
}
