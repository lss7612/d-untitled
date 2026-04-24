package com.example.demo.club.untitled.dto;

import com.example.demo.club.untitled.domain.Book;
import com.example.demo.club.untitled.domain.BookCategory;

import java.time.LocalDateTime;

/** 보유 책 카탈로그 조회/검색 응답. */
public record BookResponse(
    Long id,
    String title,
    String author,
    String category,
    String categoryLabel,
    Integer price,
    String sourceUrl,
    String thumbnailUrl,
    boolean exempt,
    LocalDateTime exemptionGrantedAt,
    int copies,
    LocalDateTime createdAt
) {
    public static BookResponse from(Book b) {
        BookCategory cat = b.getCategory();
        return new BookResponse(
            b.getId(),
            b.getTitle(),
            b.getAuthor(),
            cat == null ? null : cat.name(),
            cat == null ? null : cat.getLabel(),
            b.getPrice() == null ? null : b.getPrice().intValue(),
            b.getSourceUrl(),
            b.getThumbnailUrl(),
            b.isExempt(),
            b.getExemptionGrantedAt(),
            b.getCopies(),
            b.getCreatedAt()
        );
    }
}
