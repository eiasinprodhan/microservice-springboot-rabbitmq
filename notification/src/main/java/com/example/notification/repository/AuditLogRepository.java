package com.example.notification.repository;

import com.example.notification.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findAllByOrderByReceivedAtDesc();

    List<AuditLog> findByRoutingKeyOrderByReceivedAtDesc(String routingKey);
}
