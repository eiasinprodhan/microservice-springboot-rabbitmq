package com.example.orders.event;

import com.example.orders.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderUpdatedEvent(

        UUID eventId,

        UUID orderId,

        String productName,

        BigDecimal amount,

        String customerEmail,

        OrderStatus previousStatus,

        OrderStatus newStatus,

        LocalDateTime updatedAt
) {
}
