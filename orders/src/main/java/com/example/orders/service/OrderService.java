package com.example.orders.service;

import com.example.orders.dto.*;
import com.example.orders.entity.Order;
import com.example.orders.entity.OrderStatus;
import com.example.orders.event.OrderCancelledEvent;
import com.example.orders.event.OrderCreatedEvent;
import com.example.orders.event.OrderUpdatedEvent;
import com.example.orders.event.SystemBroadcastEvent;
import com.example.orders.exception.BadRequestException;
import com.example.orders.exception.ResourceNotFoundException;
import com.example.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order: productName={}, amount={}, customerEmail={}",
                request.productName(), request.amount(), request.customerEmail());

        Order order = Order.builder()
                .productName(request.productName())
                .amount(request.amount())
                .customerEmail(request.customerEmail())
                .status(OrderStatus.CREATED)
                .build();

        Order savedOrder = orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                savedOrder.getId(),
                savedOrder.getProductName(),
                savedOrder.getAmount(),
                savedOrder.getCustomerEmail(),
                savedOrder.getCreatedAt()
        );

        eventPublisher.publishOrderCreated(event);

        log.info("Order created and OrderCreatedEvent published: orderId={}, eventId={}",
                savedOrder.getId(), event.eventId());

        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(
            OrderStatus status,
            String customerEmail,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Order> orderPage;
        if (status != null && customerEmail != null && !customerEmail.isBlank()) {
            orderPage = orderRepository.findByStatusAndCustomerEmailIgnoreCase(
                    status, customerEmail.trim(), pageable);
        } else if (status != null) {
            orderPage = orderRepository.findByStatus(status, pageable);
        } else if (customerEmail != null && !customerEmail.isBlank()) {
            orderPage = orderRepository.findByCustomerEmailIgnoreCase(
                    customerEmail.trim(), pageable);
        } else {
            orderPage = orderRepository.findAll(pageable);
        }

        List<OrderResponse> content = orderPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                content,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        log.info("Fetching order by id: {}", id);
        Order order = findOrderOrThrow(id);
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerEmail(String customerEmail) {
        log.info("Fetching orders for customerEmail={}", customerEmail);
        return orderRepository.findByCustomerEmailOrderByCreatedAtDesc(customerEmail)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateOrder(UUID id, UpdateOrderRequest request) {
        Order order = findOrderOrThrow(id);

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException(
                    "Cannot modify an order with status: " + order.getStatus()
            );
        }

        OrderStatus previousStatus = order.getStatus();
        order.setProductName(request.productName());
        order.setAmount(request.amount());
        order.setCustomerEmail(request.customerEmail());

        Order savedOrder = orderRepository.save(order);

        OrderUpdatedEvent event = new OrderUpdatedEvent(
                UUID.randomUUID(),
                savedOrder.getId(),
                savedOrder.getProductName(),
                savedOrder.getAmount(),
                savedOrder.getCustomerEmail(),
                previousStatus,
                savedOrder.getStatus(),
                LocalDateTime.now()
        );

        eventPublisher.publishOrderUpdated(event);

        return mapToResponse(savedOrder);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID id, UpdateOrderStatusRequest request) {
        Order order = findOrderOrThrow(id);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot change status of a cancelled order");
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(request.status());

        Order savedOrder = orderRepository.save(order);

        OrderUpdatedEvent event = new OrderUpdatedEvent(
                UUID.randomUUID(),
                savedOrder.getId(),
                savedOrder.getProductName(),
                savedOrder.getAmount(),
                savedOrder.getCustomerEmail(),
                previousStatus,
                savedOrder.getStatus(),
                LocalDateTime.now()
        );

        eventPublisher.publishOrderUpdated(event);

        return mapToResponse(savedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID id, CancelOrderRequest request) {
        Order order = findOrderOrThrow(id);

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel an already completed order");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(request.reason());

        Order savedOrder = orderRepository.save(order);

        OrderCancelledEvent event = new OrderCancelledEvent(
                UUID.randomUUID(),
                savedOrder.getId(),
                savedOrder.getProductName(),
                savedOrder.getAmount(),
                savedOrder.getCustomerEmail(),
                savedOrder.getCancellationReason(),
                LocalDateTime.now()
        );

        eventPublisher.publishOrderCancelled(event);

        return mapToResponse(savedOrder);
    }

    @Transactional
    public void deleteOrder(UUID id) {
        Order order = findOrderOrThrow(id);
        orderRepository.delete(order);
        log.info("Deleted order with id: {}", id);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> republishOrderCreatedEvent(UUID id, UUID customEventId) {
        Order order = findOrderOrThrow(id);
        UUID eventIdToUse = customEventId != null ? customEventId : UUID.randomUUID();

        OrderCreatedEvent event = new OrderCreatedEvent(
                eventIdToUse,
                order.getId(),
                order.getProductName(),
                order.getAmount(),
                order.getCustomerEmail(),
                order.getCreatedAt()
        );

        eventPublisher.publishOrderCreated(event);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "OrderCreatedEvent republished to RabbitMQ");
        result.put("orderId", order.getId());
        result.put("eventId", eventIdToUse);
        return result;
    }

    public SystemBroadcastEvent broadcastSystemMessage(BroadcastMessageRequest request) {
        SystemBroadcastEvent event = new SystemBroadcastEvent(
                UUID.randomUUID(),
                request.title(),
                request.message(),
                "order-service",
                LocalDateTime.now()
        );

        eventPublisher.publishSystemBroadcast(event);
        return event;
    }

    @Transactional
    public OrderResponse simulatePoisonPillOrder(String customerEmail) {
        CreateOrderRequest poisonRequest = new CreateOrderRequest(
                "FAIL-POISON-PILL-PRODUCT",
                new BigDecimal("99.99"),
                customerEmail != null && !customerEmail.isBlank() ? customerEmail : "dlq-test@example.com"
        );
        return createOrder(poisonRequest);
    }

    @Transactional(readOnly = true)
    public OrderStatsResponse getOrderStats() {
        long totalOrders = orderRepository.count();
        BigDecimal totalRevenue = orderRepository.sumTotalRevenueExcludingStatus(OrderStatus.CANCELLED);

        Map<String, Long> countByStatus = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            countByStatus.put(status.name(), orderRepository.countByStatus(status));
        }

        return new OrderStatsResponse(totalOrders, totalRevenue, countByStatus);
    }

    private Order findOrderOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private OrderResponse mapToResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getProductName(),
                order.getAmount(),
                order.getCustomerEmail(),
                order.getStatus(),
                order.getCancellationReason(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
