package com.example.demo.club.untitled.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 회원당 월 1건 독후감 모델.
 * - report: 이번 달에 작성한 독후감 (없으면 null)
 * - myRequests: 신청한 책 목록 (작성 시 "신청한 책에서 선택" 옵션 source)
 */
public record MyBookReportsResponse(
    String targetMonth,
    LocalDate deadline,
    boolean overdue,
    BookReportResponse report,
    List<BookRequestSummary> myRequests
) {
    public record BookRequestSummary(
        Long bookRequestId,
        String title,
        String author,
        String thumbnailUrl
    ) {}
}
