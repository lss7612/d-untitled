package com.example.demo.club.untitled.controller;

import com.example.demo.club.untitled.dto.BookReportRequest;
import com.example.demo.club.untitled.dto.BookReportResponse;
import com.example.demo.club.untitled.dto.MyBookReportsResponse;
import com.example.demo.club.untitled.service.BookReportService;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.user.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/clubs/{clubId}/book-reports")
@RequiredArgsConstructor
public class BookReportController {

    private final BookReportService bookReportService;

    @GetMapping("/my")
    public ApiResponse<MyBookReportsResponse> mine(
        @PathVariable Long clubId,
        @RequestParam(required = false) String yearMonth,
        @AuthenticationPrincipal Member member
    ) {
        YearMonth ym = parse(yearMonth);
        return ApiResponse.ok(bookReportService.findMine(clubId, member.getId(), ym));
    }

    @GetMapping
    public ApiResponse<List<BookReportResponse>> list(
        @PathVariable Long clubId,
        @RequestParam(required = false) String yearMonth,
        @AuthenticationPrincipal Member member
    ) {
        return ApiResponse.ok(bookReportService.findAllByMonth(clubId, member.getId(), parse(yearMonth)));
    }

    @PostMapping
    public ApiResponse<BookReportResponse> submit(
        @PathVariable Long clubId,
        @RequestBody BookReportRequest req,
        @AuthenticationPrincipal Member member
    ) {
        return ApiResponse.ok(bookReportService.submit(clubId, member.getId(), req));
    }

    @PatchMapping("/{id}")
    public ApiResponse<BookReportResponse> update(
        @PathVariable Long clubId,
        @PathVariable Long id,
        @RequestBody BookReportRequest req,
        @AuthenticationPrincipal Member member
    ) {
        return ApiResponse.ok(bookReportService.update(clubId, member.getId(), id, req));
    }

    private YearMonth parse(String yearMonth) {
        return (yearMonth == null || yearMonth.isBlank()) ? YearMonth.now() : YearMonth.parse(yearMonth);
    }
}
