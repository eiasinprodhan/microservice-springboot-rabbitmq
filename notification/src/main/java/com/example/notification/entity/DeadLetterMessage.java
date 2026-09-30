package com.example.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dead_letter_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeadLetterMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 100)
    private String messageId;

    @Column(length = 100)
    private String originalExchange;

    @Column(length = 100)
    private String originalRoutingKey;

    @Column(length = 100)
    private String originalQueue;

    @Column(nullable = false, length = 2000)
    private String payload;

    @Column(length = 500)
    private String exceptionMessage;

    @Column(nullable = false)
    @Builder.Default
    private boolean resolved = false;

    private LocalDateTime resolvedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
