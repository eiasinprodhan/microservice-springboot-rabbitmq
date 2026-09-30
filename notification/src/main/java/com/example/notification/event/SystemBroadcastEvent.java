package com.example.notification.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record SystemBroadcastEvent(

        UUID eventId,

        String title,

        String message,

        String sourceService,

        LocalDateTime publishedAt
) {
}
