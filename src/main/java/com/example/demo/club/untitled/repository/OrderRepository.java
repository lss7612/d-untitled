package com.example.demo.club.untitled.repository;

import com.example.demo.club.untitled.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByClubIdAndTargetMonth(Long clubId, String targetMonth);
}
