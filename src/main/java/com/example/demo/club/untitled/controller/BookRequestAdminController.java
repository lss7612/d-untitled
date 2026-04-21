package com.example.demo.club.untitled.controller;

import com.example.demo.club.untitled.service.BookRequestService;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.user.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}/book-requests")
@RequiredArgsConstructor
public class BookRequestAdminController {

    private final BookRequestService bookRequestService;

    @PostMapping("/lock")
    public ApiResponse<LockResult> lock(
        @PathVariable Long clubId,
        @RequestParam(required = false) String yearMonth,
        @AuthenticationPrincipal Member member
    ) {
        YearMonth ym = parse(yearMonth);
        int count = bookRequestService.lock(clubId, member.getId(), ym);
        return ApiResponse.ok(new LockResult(ym.toString(), count));
    }

    @PostMapping("/unlock")
    public ApiResponse<LockResult> unlock(
        @PathVariable Long clubId,
        @RequestParam(required = false) String yearMonth,
        @AuthenticationPrincipal Member member
    ) {
        YearMonth ym = parse(yearMonth);
        int count = bookRequestService.unlock(clubId, member.getId(), ym);
        return ApiResponse.ok(new LockResult(ym.toString(), count));
    }

    private YearMonth parse(String yearMonth) {
        return (yearMonth == null || yearMonth.isBlank()) ? YearMonth.now() : YearMonth.parse(yearMonth);
    }

    public record LockResult(String yearMonth, int affected) {}
}
