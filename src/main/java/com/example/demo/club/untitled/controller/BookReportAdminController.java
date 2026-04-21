package com.example.demo.club.untitled.controller;

import com.example.demo.club.untitled.dto.MissingSubmittersResponse;
import com.example.demo.club.untitled.service.BookReportService;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.user.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}/book-reports")
@RequiredArgsConstructor
public class BookReportAdminController {

    private final BookReportService bookReportService;

    @GetMapping("/missing")
    public ApiResponse<MissingSubmittersResponse> missing(
        @PathVariable Long clubId,
        @RequestParam(required = false) String yearMonth,
        @AuthenticationPrincipal Member member
    ) {
        YearMonth ym = (yearMonth == null || yearMonth.isBlank()) ? YearMonth.now() : YearMonth.parse(yearMonth);
        return ApiResponse.ok(bookReportService.findMissing(clubId, member.getId(), ym));
    }
}
