package com.example.demo.club.untitled.dto;

import com.example.demo.club.untitled.domain.BookReport;
import com.example.demo.user.domain.Member;

import java.time.LocalDateTime;

public record BookReportResponse(
    Long id,
    Long bookRequestId,
    Long memberId,
    String memberName,
    String memberEmail,
    String targetMonth,
    String bookTitle,
    String bookAuthor,
    String bookThumbnailUrl,
    String title,
    String content,
    LocalDateTime submittedAt,
    LocalDateTime updatedAt
) {
    public static BookReportResponse from(BookReport r) {
        return of(r, null);
    }

    public static BookReportResponse of(BookReport r, Member m) {
        return new BookReportResponse(
            r.getId(), r.getBookRequestId(), r.getMemberId(),
            m == null ? null : m.getName(),
            m == null ? null : m.getEmail(),
            r.getTargetMonth(),
            r.getBookTitle(), r.getBookAuthor(), r.getBookThumbnailUrl(),
            r.getTitle(), r.getContent(),
            r.getSubmittedAt(), r.getUpdatedAt()
        );
    }
}
