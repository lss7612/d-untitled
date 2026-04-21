package com.example.demo.club.untitled.dto;

/**
 * 독후감 작성 payload.
 * - bookRequestId가 있으면 해당 신청 도서로 자동 매핑(소유권 검증 후 스냅샷).
 * - bookRequestId가 없으면 bookTitle 등 자유 입력 사용 (다른 책).
 */
public record BookReportRequest(
    Long bookRequestId,
    String bookTitle,
    String bookAuthor,
    String bookThumbnailUrl,
    String title,
    String content
) {}
