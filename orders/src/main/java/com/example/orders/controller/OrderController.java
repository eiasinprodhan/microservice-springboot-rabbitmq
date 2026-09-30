package com.example.orders.controller;

import com.example.orders.dto.*;
import com.example.orders.entity.OrderStatus;
import com.example.orders.event.SystemBroadcastEvent;
import com.example.orders.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    // 1. Create a new order (Publishes OrderCreatedEvent -> Topic Exchange 'order.exchange' with 'order.created')
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        log.info("POST /api/orders - Creating order for customerEmail={}", request.customerEmail());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. Get all orders with optional filtering & pagination
    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(
                orderService.getAllOrders(status, customerEmail, page, size, sortBy, sortDir)
        );
    }

    // 3. Get order statistics (total orders, total revenue, counts by status)
    @GetMapping("/stats")
    public ResponseEntity<OrderStatsResponse> getOrderStats() {
        return ResponseEntity.ok(orderService.getOrderStats());
    }

    // 4. Get orders by customer email
    @GetMapping("/customer/{email}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(
            @PathVariable String email
    ) {
        return ResponseEntity.ok(orderService.getOrdersByCustomerEmail(email));
    }

    // 5. Get single order by ID
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    // 6. Full update of order details (Publishes OrderUpdatedEvent -> 'order.updated')
    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderRequest request
    ) {
        return ResponseEntity.ok(orderService.updateOrder(id, request));
    }

    // 7. Partial update of order status (Publishes OrderUpdatedEvent -> 'order.updated')
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request));
    }

    // 8. Cancel an order with reason (Publishes OrderCancelledEvent -> 'order.cancelled')
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable UUID id,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        return ResponseEntity.ok(orderService.cancelOrder(id, request));
    }

    // 9. Delete an order
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable UUID id
    ) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    // 10. Republish OrderCreatedEvent (Optionally pass duplicate eventId to test Consumer Idempotency)
    @PostMapping("/{id}/republish")
    public ResponseEntity<Map<String, Object>> republishOrderEvent(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID eventId
    ) {
        return ResponseEntity.ok(orderService.republishOrderCreatedEvent(id, eventId));
    }

    // 11. Publish a Fanout Broadcast message to all bound queues ('order.broadcast.exchange')
    @PostMapping("/messaging/broadcast")
    public ResponseEntity<SystemBroadcastEvent> broadcastMessage(
            @Valid @RequestBody BroadcastMessageRequest request
    ) {
        SystemBroadcastEvent event = orderService.broadcastSystemMessage(request);
        return ResponseEntity.accepted().body(event);
    }

    // 12. Simulate a Poison Pill order to trigger Consumer Retries + Dead Letter Queue (DLQ)
    @PostMapping("/messaging/simulate-dlq")
    public ResponseEntity<OrderResponse> simulateDlq(
            @RequestParam(required = false) String customerEmail
    ) {
        OrderResponse response = orderService.simulatePoisonPillOrder(customerEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 13. Health / connectivity test endpoint
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Order controller is working");
    }
}
