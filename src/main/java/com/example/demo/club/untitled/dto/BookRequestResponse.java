package com.example.demo.club.untitled.dto;

import com.example.demo.club.untitled.domain.BookRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookRequestResponse(
    Long id,
    Long memberId,
    String title,
    String author,
    String publisher,
    String isbn,
    Integer price,
    BigDecimal originalPrice,
    String currency,
    BigDecimal exchangeRate,
    String category,
    String categoryLabel,
    String sourceUrl,
    String thumbnailUrl,
    String status,
    String statusLabel,
    String targetMonth,
    LocalDateTime createdAt,
    LocalDateTime arrivedAt,
    LocalDateTime receivedAt
) {
    public static BookRequestResponse from(BookRequest br) {
        return new BookRequestResponse(
            br.getId(),
            br.getMemberId(),
            br.getTitle(),
            br.getAuthor(),
            br.getPublisher(),
            br.getIsbn(),
            br.getPrice(),
            br.getOriginalPrice(),
            br.getCurrency(),
            br.getExchangeRate(),
            br.getCategory().name(),
            br.getCategory().getLabel(),
            br.getSourceUrl(),
            br.getThumbnailUrl(),
            br.getStatus().name(),
            br.getStatus().getLabel(),
            br.getTargetMonth(),
            br.getCreatedAt(),
            br.getArrivedAt(),
            br.getReceivedAt()
        );
    }
}
