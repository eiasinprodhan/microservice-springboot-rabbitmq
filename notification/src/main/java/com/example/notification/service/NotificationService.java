package com.example.notification.service;

import com.example.notification.entity.Notification;
import com.example.notification.event.OrderCreatedEvent;
import com.example.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void processOrderCreated(
            OrderCreatedEvent event
    ) {

        if (notificationRepository
                .existsByEventId(event.eventId())) {

            log.info(
                    "Duplicate event ignored. eventId={}",
                    event.eventId()
            );

            return;
        }

        String message =
                "Your order " +
                        event.orderId() +
                        " for product '" +
                        event.productName() +
                        "' has been created successfully.";

        Notification notification =
                Notification.builder()
                        .eventId(event.eventId())
                        .orderId(event.orderId())
                        .customerEmail(event.customerEmail())
                        .message(message)
                        .build();

        notificationRepository.save(notification);

        log.info(
                "Notification created. orderId={}, eventId={}",
                event.orderId(),
                event.eventId()
        );
    }
}
