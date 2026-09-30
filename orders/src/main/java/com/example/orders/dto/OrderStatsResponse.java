package com.example.orders.dto;

import java.math.BigDecimal;
import java.util.Map;

public record OrderStatsResponse(

        long totalOrders,

        BigDecimal totalRevenue,

        Map<String, Long> countByStatus
) {
}
