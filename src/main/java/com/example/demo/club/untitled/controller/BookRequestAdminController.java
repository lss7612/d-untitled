package com.example.demo.club.untitled.controller;

import com.example.demo.club.untitled.dto.MarkRequest;
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
        boolean locked = bookRequestService.lock(clubId, member.getId(), ym, member);
        return ApiResponse.ok(new LockResult(ym.toString(), locked));
    }

    @PostMapping("/unlock")
    public ApiResponse<LockResult> unlock(
        @PathVariable Long clubId,
        @RequestParam(required = false) String yearMonth,
        @AuthenticationPrincipal Member member
    ) {
        YearMonth ym = parse(yearMonth);
        boolean locked = bookRequestService.unlock(clubId, member.getId(), ym, member);
        return ApiResponse.ok(new LockResult(ym.toString(), locked));
    }

    @PatchMapping("/mark-arrived")
    public ApiResponse<MarkResult> markArrived(
        @PathVariable Long clubId,
        @RequestBody MarkRequest req,
        @AuthenticationPrincipal Member member
    ) {
        int count = bookRequestService.markArrived(clubId, member.getId(), req.ids());
        return ApiResponse.ok(new MarkResult(count));
    }

    @PatchMapping("/mark-unarrived")
    public ApiResponse<MarkResult> markUnarrived(
        @PathVariable Long clubId,
        @RequestBody MarkRequest req,
        @AuthenticationPrincipal Member member
    ) {
        int count = bookRequestService.markUnarrived(clubId, member.getId(), req.ids());
        return ApiResponse.ok(new MarkResult(count));
    }

    private YearMonth parse(String yearMonth) {
        return (yearMonth == null || yearMonth.isBlank()) ? YearMonth.now() : YearMonth.parse(yearMonth);
    }

    public record LockResult(String yearMonth, boolean locked) {}
    public record MarkResult(int affected) {}
}
