package com.example.demo.club.untitled.dto;

import com.example.demo.club.untitled.domain.Order;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    Long clubId,
    String targetMonth,
    String status,
    Integer totalAmount,
    Integer totalQuantity,
    Long createdBy,
    LocalDateTime createdAt,
    LocalDateTime orderedAt,
    List<OrderItemResponse> items
) {
    public static OrderResponse of(Order o, List<OrderItemResponse> items) {
        return new OrderResponse(
            o.getId(), o.getClubId(), o.getTargetMonth(),
            o.getStatus().name(),
            o.getTotalAmount(), o.getTotalQuantity(),
            o.getCreatedBy(),
            o.getCreatedAt(), o.getOrderedAt(),
            items
        );
    }
}
