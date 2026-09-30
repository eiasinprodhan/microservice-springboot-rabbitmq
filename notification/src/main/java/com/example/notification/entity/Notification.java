package com.example.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notification_event_id", columnList = "event_id", unique = true),
                @Index(name = "idx_notification_order_id", columnList = "order_id"),
                @Index(name = "idx_notification_customer_email", columnList = "customer_email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private NotificationType type = NotificationType.ORDER_CREATED;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false;

    private LocalDateTime readAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
