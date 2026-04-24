package com.example.demo.club.untitled.dto;

import com.example.demo.club.untitled.domain.Book;
import com.example.demo.club.untitled.domain.BookExemptionRequest;
import com.example.demo.user.domain.Member;

import java.time.LocalDateTime;

public record BookExemptionResponse(
    Long id,
    Long clubId,
    Long bookId,
    String bookTitle,
    String bookAuthor,
    Long memberId,
    String memberName,
    String memberEmail,
    String reason,
    String status,
    LocalDateTime createdAt,
    LocalDateTime processedAt
) {
    public static BookExemptionResponse of(BookExemptionRequest req, Book book, Member member) {
        return new BookExemptionResponse(
            req.getId(),
            req.getClubId(),
            req.getBookId(),
            book == null ? null : book.getTitle(),
            book == null ? null : book.getAuthor(),
            req.getMemberId(),
            member == null ? null : member.getName(),
            member == null ? null : member.getEmail(),
            req.getReason(),
            req.getStatus().name(),
            req.getCreatedAt(),
            req.getProcessedAt()
        );
    }
}
