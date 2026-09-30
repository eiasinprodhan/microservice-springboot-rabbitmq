package com.example.notification.service;

import com.example.notification.config.RabbitMQConfig;
import com.example.notification.entity.AuditLog;
import com.example.notification.entity.DeadLetterMessage;
import com.example.notification.entity.NotificationType;
import com.example.notification.event.OrderCancelledEvent;
import com.example.notification.event.OrderCreatedEvent;
import com.example.notification.event.OrderUpdatedEvent;
import com.example.notification.event.SystemBroadcastEvent;
import com.example.notification.repository.AuditLogRepository;
import com.example.notification.repository.DeadLetterMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedEventListener {

    private final NotificationService notificationService;
    private final AuditLogRepository auditLogRepository;
    private final DeadLetterMessageRepository dlqRepository;

    // 1. Topic Exchange Consumer: order.created
    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void consumeOrderCreated(OrderCreatedEvent event) {
        log.info("[Topic Consumer] Received OrderCreatedEvent: eventId={}, orderId={}, product={}",
                event.eventId(), event.orderId(), event.productName());

        notificationService.processOrderCreated(event);
    }

    // 2. Topic Exchange Consumer: order.updated
    @RabbitListener(queues = RabbitMQConfig.ORDER_UPDATED_QUEUE)
    public void consumeOrderUpdated(OrderUpdatedEvent event) {
        log.info("[Topic Consumer] Received OrderUpdatedEvent: eventId={}, orderId={}, status={}->{}",
                event.eventId(), event.orderId(), event.previousStatus(), event.newStatus());

        notificationService.processOrderUpdated(event);
    }

    // 3. Topic Exchange Consumer: order.cancelled
    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCELLED_QUEUE)
    public void consumeOrderCancelled(OrderCancelledEvent event) {
        log.info("[Topic Consumer] Received OrderCancelledEvent: eventId={}, orderId={}, reason={}",
                event.eventId(), event.orderId(), event.cancellationReason());

        notificationService.processOrderCancelled(event);
    }

    // 4. Fanout Exchange Consumer #1: Email Broadcast Channel
    @RabbitListener(queues = RabbitMQConfig.BROADCAST_EMAIL_QUEUE)
    public void consumeEmailBroadcast(SystemBroadcastEvent event) {
        log.info("[Fanout Email Consumer] Received SystemBroadcastEvent: eventId={}, title={}",
                event.eventId(), event.title());

        notificationService.processBroadcast(event, NotificationType.SYSTEM_BROADCAST_EMAIL);
    }

    // 5. Fanout Exchange Consumer #2: SMS Broadcast Channel
    @RabbitListener(queues = RabbitMQConfig.BROADCAST_SMS_QUEUE)
    public void consumeSmsBroadcast(SystemBroadcastEvent event) {
        log.info("[Fanout SMS Consumer] Received SystemBroadcastEvent: eventId={}, title={}",
                event.eventId(), event.title());

        notificationService.processBroadcast(event, NotificationType.SYSTEM_BROADCAST_SMS);
    }

    // 6. Topic Wildcard Consumer ('order.*'): Captures all order events for auditing
    @RabbitListener(queues = RabbitMQConfig.ORDER_AUDIT_QUEUE)
    public void consumeOrderWildcardAudit(
            Message message,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey,
            @Header(value = "X-Event-Type", required = false) String eventType
    ) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        String correlationId = message.getMessageProperties().getCorrelationId();

        log.info("[Wildcard Audit Consumer] Captured '{}' event (type={}, correlationId={})",
                routingKey, eventType, correlationId);

        AuditLog auditLog = AuditLog.builder()
                .routingKey(routingKey)
                .eventType(eventType != null ? eventType : "UNKNOWN")
                .correlationId(correlationId)
                .payload(payload.length() > 2000 ? payload.substring(0, 2000) : payload)
                .build();

        auditLogRepository.save(auditLog);
    }

    // 7. Dead Letter Queue (DLQ) Consumer: Captures rejected/failed messages after retry exhaustion
    @RabbitListener(queues = RabbitMQConfig.ORDER_DLQ)
    @SuppressWarnings("unchecked")
    public void consumeDeadLetterQueue(Message message) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> headers = message.getMessageProperties().getHeaders();

        String originalExchange = "order.exchange";
        String originalRoutingKey = "unknown";
        String originalQueue = "unknown";

        Object xDeathHeader = headers.get("x-death");
        if (xDeathHeader instanceof List<?> xDeathList && !xDeathList.isEmpty()) {
            Object firstDeath = xDeathList.getFirst();
            if (firstDeath instanceof Map<?, ?> deathMap) {
                if (deathMap.get("exchange") != null) {
                    originalExchange = deathMap.get("exchange").toString();
                }
                if (deathMap.get("queue") != null) {
                    originalQueue = deathMap.get("queue").toString();
                }
                if (deathMap.get("routing-keys") instanceof List<?> rkList && !rkList.isEmpty()) {
                    originalRoutingKey = rkList.getFirst().toString();
                }
            }
        }

        log.error("[DLQ Consumer] Captured Dead-Lettered Message from queue='{}', routingKey='{}', payload={}",
                originalQueue, originalRoutingKey, payload);

        DeadLetterMessage dlqRecord = DeadLetterMessage.builder()
                .messageId(message.getMessageProperties().getMessageId())
                .originalExchange(originalExchange)
                .originalRoutingKey(originalRoutingKey)
                .originalQueue(originalQueue)
                .payload(payload.length() > 2000 ? payload.substring(0, 2000) : payload)
                .exceptionMessage("Message rejected after exhausting max retry attempts")
                .resolved(false)
                .build();

        dlqRepository.save(dlqRecord);
    }
}
