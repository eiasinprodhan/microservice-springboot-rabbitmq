package com.example.orders.service;

import com.example.orders.config.RabbitMQConfig;
import com.example.orders.entity.Order;
import com.example.orders.entity.OrderStatus;
import com.example.orders.event.NotificationProcessedEvent;
import com.example.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCallbackListener {

    private final OrderRepository orderRepository;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CALLBACK_QUEUE)
    @Transactional
    public void onNotificationProcessed(NotificationProcessedEvent event) {
        log.info("[Callback Listener] Received NotificationProcessedEvent: orderId={}, notificationId={}, status={}",
                event.orderId(), event.notificationId(), event.status());

        orderRepository.findById(event.orderId()).ifPresentOrElse(order -> {
            if (order.getStatus() == OrderStatus.CREATED) {
                order.setStatus(OrderStatus.PROCESSING);
                orderRepository.save(order);
                log.info("[Callback Listener] Order {} status transitioned CREATED -> PROCESSING after notification confirmation",
                        order.getId());
            }
        }, () -> log.warn("[Callback Listener] Order not found for callback event: orderId={}", event.orderId()));
    }
}
