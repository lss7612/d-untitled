package com.example.demo.club.untitled.dto;

import com.example.demo.club.untitled.domain.OrderItem;

public record OrderItemResponse(
    Long id,
    String isbn,
    String aladinItemCode,
    String title,
    String author,
    Integer unitPrice,
    Integer quantity,
    Integer subtotal
) {
    public static OrderItemResponse from(OrderItem oi) {
        return new OrderItemResponse(
            oi.getId(), oi.getIsbn(), oi.getAladinItemCode(),
            oi.getTitle(), oi.getAuthor(),
            oi.getUnitPrice(), oi.getQuantity(), oi.getSubtotal()
        );
    }
}
