package com.example.notification.repository;

import com.example.notification.entity.DeadLetterMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeadLetterMessageRepository extends JpaRepository<DeadLetterMessage, UUID> {

    List<DeadLetterMessage> findAllByOrderByCreatedAtDesc();

    List<DeadLetterMessage> findByResolvedOrderByCreatedAtDesc(boolean resolved);

    long countByResolved(boolean resolved);
}
