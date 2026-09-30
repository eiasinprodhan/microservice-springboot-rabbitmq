package com.example.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String routingKey;

    @Column(length = 100)
    private String eventType;

    @Column(length = 100)
    private String correlationId;

    @Column(nullable = false, length = 2000)
    private String payload;

    @Column(nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @PrePersist
    protected void onCreate() {
        this.receivedAt = LocalDateTime.now();
    }
}
