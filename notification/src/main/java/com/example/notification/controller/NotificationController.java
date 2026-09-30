package com.example.notification.controller;

import com.example.notification.dto.CreateNotificationRequest;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.dto.NotificationStatsResponse;
import com.example.notification.dto.PageResponse;
import com.example.notification.entity.AuditLog;
import com.example.notification.entity.DeadLetterMessage;
import com.example.notification.entity.NotificationType;
import com.example.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 1. Get all notifications (Paginated & Filterable by customerEmail, isRead, type)
    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                notificationService.getNotifications(customerEmail, isRead, type, page, size)
        );
    }

    // 2. Get notification, DLQ, and Audit statistics
    @GetMapping("/stats")
    public ResponseEntity<NotificationStatsResponse> getStats() {
        return ResponseEntity.ok(notificationService.getStats());
    }

    // 3. Get notifications by Order ID
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByOrder(
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(notificationService.getNotificationsByOrderId(orderId));
    }

    // 4. Get notifications by Customer Email
    @GetMapping("/customer/{email}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByCustomer(
            @PathVariable String email
    ) {
        return ResponseEntity.ok(notificationService.getNotificationsByCustomerEmail(email));
    }

    // 5. Get one notification by ID
    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotification(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(notificationService.getNotification(id));
    }

    // 6. Create a custom notification manually
    @PostMapping
    public ResponseEntity<NotificationResponse> createCustomNotification(
            @Valid @RequestBody CreateNotificationRequest request
    ) {
        NotificationResponse response = notificationService.createCustomNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 7. Mark a single notification as read
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    // 8. Mark all notifications as read for a customer email
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @RequestParam String customerEmail
    ) {
        return ResponseEntity.ok(notificationService.markAllAsReadForCustomer(customerEmail));
    }

    // 9. Delete a notification by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID id
    ) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    // ================= DEAD LETTER QUEUE (DLQ) ENDPOINTS =================

    // 10. List all Dead Letter Queue messages
    @GetMapping("/dlq")
    public ResponseEntity<List<DeadLetterMessage>> getDeadLetterMessages(
            @RequestParam(required = false) Boolean resolved
    ) {
        return ResponseEntity.ok(notificationService.getDeadLetterMessages(resolved));
    }

    // 11. Get a single Dead Letter Queue message by ID
    @GetMapping("/dlq/{id}")
    public ResponseEntity<DeadLetterMessage> getDeadLetterMessage(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(notificationService.getDeadLetterMessage(id));
    }

    // 12. Mark a Dead Letter Queue message as resolved
    @PatchMapping("/dlq/{id}/resolve")
    public ResponseEntity<DeadLetterMessage> resolveDeadLetterMessage(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(notificationService.resolveDeadLetterMessage(id));
    }

    // 13. Delete a Dead Letter Queue message
    @DeleteMapping("/dlq/{id}")
    public ResponseEntity<Void> deleteDeadLetterMessage(
            @PathVariable UUID id
    ) {
        notificationService.deleteDeadLetterMessage(id);
        return ResponseEntity.noContent().build();
    }

    // ================= TOPIC WILDCARD AUDIT LOG ENDPOINTS =================

    // 14. Get all wildcard audit logs ('order.*')
    @GetMapping("/audit")
    public ResponseEntity<List<AuditLog>> getAuditLogs(
            @RequestParam(required = false) String routingKey
    ) {
        return ResponseEntity.ok(notificationService.getAuditLogs(routingKey));
    }

    // 15. Health / connectivity test endpoint
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Notification controller is working");
    }
}
