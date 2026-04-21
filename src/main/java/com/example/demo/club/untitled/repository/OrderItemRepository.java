package com.example.demo.club.untitled.repository;

import com.example.demo.club.untitled.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findAllByOrderId(Long orderId);
    Optional<OrderItem> findByOrderIdAndIsbn(Long orderId, String isbn);
}
