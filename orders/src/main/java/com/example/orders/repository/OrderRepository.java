package com.example.orders.repository;

import com.example.orders.entity.Order;
import com.example.orders.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findByCustomerEmailIgnoreCase(String customerEmail, Pageable pageable);

    Page<Order> findByStatusAndCustomerEmailIgnoreCase(
            OrderStatus status,
            String customerEmail,
            Pageable pageable
    );

    long countByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.status <> :excludedStatus")
    BigDecimal sumTotalRevenueExcludingStatus(@Param("excludedStatus") OrderStatus excludedStatus);
}
