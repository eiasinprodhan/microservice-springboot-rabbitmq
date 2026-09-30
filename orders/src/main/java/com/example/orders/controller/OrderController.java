package com.example.orders.controller;

import com.example.orders.dto.CreateOrderRequest;
import com.example.orders.dto.OrderResponse;
import com.example.orders.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {

        log.info("========== CREATE ORDER REQUEST ==========");
        log.info("Product: {}", request.productName());
        log.info("Amount: {}", request.amount());
        log.info("Customer Email: {}", request.customerEmail());

        OrderResponse response = orderService.createOrder(request);

        log.info("========== CREATE ORDER SUCCESS ==========");
        log.info("Order ID: {}", response.id());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID id
    ) {

        log.info("========== GET ORDER ==========");
        log.info("Order ID: {}", id);

        OrderResponse response = orderService.getOrder(id);

        log.info("Order found: {}", id);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/test")
    public ResponseEntity<String> test() {

        System.out.println("===== TEST ENDPOINT HIT =====");

        return ResponseEntity.ok("Order controller is working");
    }


}
