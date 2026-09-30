package com.example.notification.dto;

import java.util.Map;

public record NotificationStatsResponse(

        long totalNotifications,

        long unreadCount,

        long readCount,

        long deadLetterQueueCount,

        long unresolvedDlqCount,

        long totalAuditLogs,

        Map<String, Long> countByType
) {
}
