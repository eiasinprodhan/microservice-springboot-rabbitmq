package com.example.orders.service;

import com.example.orders.dto.CreateOrderRequest;
import com.example.orders.dto.OrderResponse;
import com.example.orders.entity.Order;
import com.example.orders.event.OrderCreatedEvent;
import com.example.orders.exception.ResourceNotFoundException;
import com.example.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.example.orders.config.RabbitMQConfig.ORDER_CREATED_ROUTING_KEY;
import static com.example.orders.config.RabbitMQConfig.ORDER_EXCHANGE;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        log.info("========== SERVICE ENTERED ==========");

        log.info(
                "Creating order: productName={}, amount={}, customerEmail={}",
                request.productName(),
                request.amount(),
                request.customerEmail()
        );

        // 1. Create entity
        Order order = Order.builder()
                .productName(request.productName())
                .amount(request.amount())
                .customerEmail(request.customerEmail())
                .build();

        log.info("Order entity created: {}", order);

        // 2. Save to database
        log.info("Saving order to database...");

        Order savedOrder = orderRepository.save(order);

        log.info(
                "Order saved successfully. id={}, createdAt={}",
                savedOrder.getId(),
                savedOrder.getCreatedAt()
        );

        // 3. Create RabbitMQ event
        log.info("Creating OrderCreatedEvent...");

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                savedOrder.getId(),
                savedOrder.getProductName(),
                savedOrder.getAmount(),
                savedOrder.getCustomerEmail(),
                savedOrder.getCreatedAt()
        );

        log.info("OrderCreatedEvent created: {}", event);

        // 4. Publish event
        log.info(
                "Publishing event to RabbitMQ. exchange={}, routingKey={}",
                ORDER_EXCHANGE,
                ORDER_CREATED_ROUTING_KEY
        );

        rabbitTemplate.convertAndSend(
                ORDER_EXCHANGE,
                ORDER_CREATED_ROUTING_KEY,
                event
        );

        log.info("RabbitMQ event published successfully");

        // 5. Return response
        OrderResponse response = mapToResponse(savedOrder);

        log.info(
                "========== ORDER CREATED SUCCESSFULLY ==========" +
                        " orderId={}",
                savedOrder.getId()
        );

        return response;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {

        log.info("Searching for order: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with id: " + id
                        )
                );

        log.info("Order found: {}", id);

        return mapToResponse(order);
    }

    private OrderResponse mapToResponse(Order order) {

        log.info("Mapping Order entity to OrderResponse");

        return new OrderResponse(
                order.getId(),
                order.getProductName(),
                order.getAmount(),
                order.getCustomerEmail(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
