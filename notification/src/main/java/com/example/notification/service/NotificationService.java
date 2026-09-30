package com.example.notification.service;

import com.example.notification.config.RabbitMQConfig;
import com.example.notification.dto.CreateNotificationRequest;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.dto.NotificationStatsResponse;
import com.example.notification.dto.PageResponse;
import com.example.notification.entity.AuditLog;
import com.example.notification.entity.DeadLetterMessage;
import com.example.notification.entity.Notification;
import com.example.notification.entity.NotificationType;
import com.example.notification.event.*;
import com.example.notification.exception.ResourceNotFoundException;
import com.example.notification.repository.AuditLogRepository;
import com.example.notification.repository.DeadLetterMessageRepository;
import com.example.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeadLetterMessageRepository dlqRepository;
    private final AuditLogRepository auditLogRepository;
    private final RabbitTemplate rabbitTemplate;

    // ================= RABBITMQ EVENT PROCESSORS =================

    @Transactional
    public void processOrderCreated(OrderCreatedEvent event) {
        if (event.productName() != null && event.productName().toUpperCase().contains("FAIL")) {
            log.error("Simulating processing failure for poison pill product: {}", event.productName());
            throw new IllegalStateException(
                    "Simulated consumer failure for poison pill product: " + event.productName()
            );
        }

        if (notificationRepository.existsByEventId(event.eventId())) {
            log.info("[Idempotency Check] Duplicate OrderCreatedEvent ignored. eventId={}", event.eventId());
            return;
        }

        String message = String.format(
                "Your order %s for product '%s' ($%s) has been created successfully.",
                event.orderId(),
                event.productName(),
                event.amount()
        );

        Notification notification = Notification.builder()
                .eventId(event.eventId())
                .orderId(event.orderId())
                .customerEmail(event.customerEmail())
                .type(NotificationType.ORDER_CREATED)
                .message(message)
                .build();

        Notification saved = notificationRepository.save(notification);

        log.info("Notification saved for OrderCreatedEvent: notificationId={}, orderId={}",
                saved.getId(), event.orderId());

        // Publish callback event via Direct Exchange back to order-service
        NotificationProcessedEvent callbackEvent = new NotificationProcessedEvent(
                UUID.randomUUID(),
                event.orderId(),
                saved.getId(),
                "DELIVERED",
                LocalDateTime.now()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_CALLBACK_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_PROCESSED_ROUTING_KEY,
                callbackEvent
        );

        log.info("Published NotificationProcessedEvent callback to order-service for orderId={}",
                event.orderId());
    }

    @Transactional
    public void processOrderUpdated(OrderUpdatedEvent event) {
        if (notificationRepository.existsByEventId(event.eventId())) {
            log.info("[Idempotency Check] Duplicate OrderUpdatedEvent ignored. eventId={}", event.eventId());
            return;
        }

        String message = String.format(
                "Order %s ('%s') was updated. Status: %s -> %s, Amount: $%s",
                event.orderId(),
                event.productName(),
                event.previousStatus(),
                event.newStatus(),
                event.amount()
        );

        Notification notification = Notification.builder()
                .eventId(event.eventId())
                .orderId(event.orderId())
                .customerEmail(event.customerEmail())
                .type(NotificationType.ORDER_UPDATED)
                .message(message)
                .build();

        notificationRepository.save(notification);
        log.info("Notification created for OrderUpdatedEvent: orderId={}, eventId={}",
                event.orderId(), event.eventId());
    }

    @Transactional
    public void processOrderCancelled(OrderCancelledEvent event) {
        if (notificationRepository.existsByEventId(event.eventId())) {
            log.info("[Idempotency Check] Duplicate OrderCancelledEvent ignored. eventId={}", event.eventId());
            return;
        }

        String message = String.format(
                "Order %s for '%s' has been cancelled. Reason: %s",
                event.orderId(),
                event.productName(),
                event.cancellationReason()
        );

        Notification notification = Notification.builder()
                .eventId(event.eventId())
                .orderId(event.orderId())
                .customerEmail(event.customerEmail())
                .type(NotificationType.ORDER_CANCELLED)
                .message(message)
                .build();

        notificationRepository.save(notification);
        log.info("Notification created for OrderCancelledEvent: orderId={}, eventId={}",
                event.orderId(), event.eventId());
    }

    @Transactional
    public void processBroadcast(SystemBroadcastEvent event, NotificationType channelType) {
        // Derive deterministic unique eventId per fanout channel so both Email & SMS queues can store their copy
        UUID channelEventId = UUID.nameUUIDFromBytes(
                (event.eventId().toString() + ":" + channelType.name()).getBytes()
        );

        if (notificationRepository.existsByEventId(channelEventId)) {
            log.info("[Idempotency Check] Duplicate broadcast ignored. eventId={}", channelEventId);
            return;
        }

        String message = String.format("[%s BROADCAST] %s: %s",
                channelType == NotificationType.SYSTEM_BROADCAST_EMAIL ? "EMAIL" : "SMS",
                event.title(),
                event.message()
        );

        Notification notification = Notification.builder()
                .eventId(channelEventId)
                .orderId(null)
                .customerEmail("all-subscribers@example.com")
                .type(channelType)
                .message(message)
                .build();

        notificationRepository.save(notification);
        log.info("Processed Fanout broadcast on channel {}: eventId={}", channelType, channelEventId);
    }

    // ================= REST API OPERATIONS =================

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(
            String customerEmail,
            Boolean isRead,
            NotificationType type,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> notificationPage;

        if (customerEmail != null && !customerEmail.isBlank()) {
            notificationPage = notificationRepository.findByCustomerEmailIgnoreCase(
                    customerEmail.trim(), pageable);
        } else if (isRead != null) {
            notificationPage = notificationRepository.findByIsRead(isRead, pageable);
        } else if (type != null) {
            notificationPage = notificationRepository.findByType(type, pageable);
        } else {
            notificationPage = notificationRepository.findAll(pageable);
        }

        List<NotificationResponse> content = notificationPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                content,
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages(),
                notificationPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotification(UUID id) {
        Notification notification = findNotificationOrThrow(id);
        return mapToResponse(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByOrderId(UUID orderId) {
        return notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByCustomerEmail(String email) {
        return notificationRepository.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(email)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public NotificationResponse createCustomNotification(CreateNotificationRequest request) {
        Notification notification = Notification.builder()
                .eventId(UUID.randomUUID())
                .orderId(request.orderId())
                .customerEmail(request.customerEmail())
                .type(NotificationType.CUSTOM)
                .message(request.message())
                .build();

        return mapToResponse(notificationRepository.save(notification));
    }

    @Transactional
    public NotificationResponse markAsRead(UUID id) {
        Notification notification = findNotificationOrThrow(id);
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }
        return mapToResponse(notification);
    }

    @Transactional
    public Map<String, Object> markAllAsReadForCustomer(String customerEmail) {
        List<Notification> unread = notificationRepository
                .findByCustomerEmailIgnoreCaseAndIsReadFalse(customerEmail);
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(n -> {
            n.setRead(true);
            n.setReadAt(now);
        });
        notificationRepository.saveAll(unread);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customerEmail", customerEmail);
        result.put("markedReadCount", unread.size());
        return result;
    }

    @Transactional
    public void deleteNotification(UUID id) {
        Notification notification = findNotificationOrThrow(id);
        notificationRepository.delete(notification);
    }

    @Transactional(readOnly = true)
    public NotificationStatsResponse getStats() {
        long total = notificationRepository.count();
        long unread = notificationRepository.countByIsRead(false);
        long read = notificationRepository.countByIsRead(true);
        long totalDlq = dlqRepository.count();
        long unresolvedDlq = dlqRepository.countByResolved(false);
        long totalAudit = auditLogRepository.count();

        Map<String, Long> countByType = new LinkedHashMap<>();
        for (NotificationType type : NotificationType.values()) {
            countByType.put(type.name(), notificationRepository.countByType(type));
        }

        return new NotificationStatsResponse(
                total, unread, read, totalDlq, unresolvedDlq, totalAudit, countByType
        );
    }

    // ================= DEAD LETTER QUEUE (DLQ) & AUDIT LOG OPERATIONS =================

    @Transactional(readOnly = true)
    public List<DeadLetterMessage> getDeadLetterMessages(Boolean resolved) {
        if (resolved != null) {
            return dlqRepository.findByResolvedOrderByCreatedAtDesc(resolved);
        }
        return dlqRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public DeadLetterMessage getDeadLetterMessage(UUID id) {
        return dlqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DLQ message not found with id: " + id));
    }

    @Transactional
    public DeadLetterMessage resolveDeadLetterMessage(UUID id) {
        DeadLetterMessage dlqMessage = getDeadLetterMessage(id);
        dlqMessage.setResolved(true);
        dlqMessage.setResolvedAt(LocalDateTime.now());
        return dlqRepository.save(dlqMessage);
    }

    @Transactional
    public void deleteDeadLetterMessage(UUID id) {
        DeadLetterMessage dlqMessage = getDeadLetterMessage(id);
        dlqRepository.delete(dlqMessage);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogs(String routingKey) {
        if (routingKey != null && !routingKey.isBlank()) {
            return auditLogRepository.findByRoutingKeyOrderByReceivedAtDesc(routingKey.trim());
        }
        return auditLogRepository.findAllByOrderByReceivedAtDesc();
    }

    private Notification findNotificationOrThrow(UUID id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
    }

    public NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getEventId(),
                notification.getOrderId(),
                notification.getCustomerEmail(),
                notification.getType(),
                notification.getMessage(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
