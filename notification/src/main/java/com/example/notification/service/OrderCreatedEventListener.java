package com.example.notification.service;

import com.example.notification.config.RabbitMQConfig;
import com.example.notification.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedEventListener {

    private final NotificationService notificationService;

    @RabbitListener(
            queues = RabbitMQConfig.ORDER_CREATED_QUEUE
    )
    public void consume(OrderCreatedEvent event) {

        log.info(
                "Received OrderCreatedEvent. eventId={}, orderId={}",
                event.eventId(),
                event.orderId()
        );

        notificationService.processOrderCreated(event);
    }
}
