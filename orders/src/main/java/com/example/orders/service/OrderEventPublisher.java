package com.example.orders.service;

import com.example.orders.config.RabbitMQConfig;
import com.example.orders.event.OrderCancelledEvent;
import com.example.orders.event.OrderCreatedEvent;
import com.example.orders.event.OrderUpdatedEvent;
import com.example.orders.event.SystemBroadcastEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        publishToTopic(
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                event.eventId(),
                "OrderCreatedEvent",
                event
        );
    }

    public void publishOrderUpdated(OrderUpdatedEvent event) {
        publishToTopic(
                RabbitMQConfig.ORDER_UPDATED_ROUTING_KEY,
                event.eventId(),
                "OrderUpdatedEvent",
                event
        );
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        publishToTopic(
                RabbitMQConfig.ORDER_CANCELLED_ROUTING_KEY,
                event.eventId(),
                "OrderCancelledEvent",
                event
        );
    }

    public void publishSystemBroadcast(SystemBroadcastEvent event) {
        CorrelationData correlationData = new CorrelationData(event.eventId().toString());

        log.info("Publishing Fanout broadcast event: eventId={}, title={}",
                event.eventId(), event.title());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_BROADCAST_EXCHANGE,
                "", // Fanout exchange ignores routing key
                event,
                message -> {
                    message.getMessageProperties().setMessageId(event.eventId().toString());
                    message.getMessageProperties().setCorrelationId(event.eventId().toString());
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setHeader("X-Event-Type", "SystemBroadcastEvent");
                    message.getMessageProperties().setHeader("X-Source-Service", "order-service");
                    return message;
                },
                correlationData
        );
    }

    private void publishToTopic(
            String routingKey,
            UUID eventId,
            String eventType,
            Object payload
    ) {
        CorrelationData correlationData = new CorrelationData(eventId.toString());

        log.info("Publishing {} to exchange='{}', routingKey='{}', eventId={}",
                eventType, RabbitMQConfig.ORDER_EXCHANGE, routingKey, eventId);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                routingKey,
                payload,
                message -> {
                    message.getMessageProperties().setMessageId(eventId.toString());
                    message.getMessageProperties().setCorrelationId(eventId.toString());
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setHeader("X-Event-Type", eventType);
                    message.getMessageProperties().setHeader("X-Source-Service", "order-service");
                    return message;
                },
                correlationData
        );
    }
}
