package com.example.orders.dto;

import com.example.orders.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(

        UUID id,

        String productName,

        BigDecimal amount,

        String customerEmail,

        OrderStatus status,

        LocalDateTime createdAt
) {
}
