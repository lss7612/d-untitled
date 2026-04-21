package com.example.demo.club.untitled.dto;

import com.example.demo.club.untitled.domain.BookRequest;
import com.example.demo.user.domain.Member;

public record AdminBookRequestRow(
    Long id,
    Long memberId,
    String memberName,
    String memberEmail,
    String title,
    String author,
    String isbn,
    String aladinItemCode,
    Integer price,
    String category,
    String categoryLabel,
    String sourceUrl,
    String thumbnailUrl,
    String status,
    String statusLabel,
    Long orderId
) {
    public static AdminBookRequestRow of(BookRequest br, Member m) {
        return new AdminBookRequestRow(
            br.getId(),
            br.getMemberId(),
            m == null ? null : m.getName(),
            m == null ? null : m.getEmail(),
            br.getTitle(),
            br.getAuthor(),
            br.getIsbn(),
            br.getAladinItemCode(),
            br.getPrice(),
            br.getCategory().name(),
            br.getCategory().getLabel(),
            br.getSourceUrl(),
            br.getThumbnailUrl(),
            br.getStatus().name(),
            br.getStatus().getLabel(),
            br.getOrderId()
        );
    }
}
