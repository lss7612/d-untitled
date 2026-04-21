package com.example.demo.club.untitled.controller;

import com.example.demo.club.untitled.dto.AdminBookRequestRow;
import com.example.demo.club.untitled.dto.MarkRequest;
import com.example.demo.club.untitled.dto.OrderResponse;
import com.example.demo.club.untitled.service.OrderService;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.user.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderService orderService;

    @GetMapping("/book-requests/all")
    public ApiResponse<List<AdminBookRequestRow>> listAll(
        @PathVariable Long clubId,
        @RequestParam(required = false) String yearMonth,
        @AuthenticationPrincipal Member member
    ) {
        return ApiResponse.ok(orderService.findAllRequests(clubId, member.getId(), parse(yearMonth)));
    }

    @GetMapping("/orders")
    public ApiResponse<OrderResponse> get(
        @PathVariable Long clubId,
        @RequestParam(required = false) String yearMonth,
        @AuthenticationPrincipal Member member
    ) {
        return ApiResponse.ok(orderService.findOrder(clubId, member.getId(), parse(yearMonth)).orElse(null));
    }

    /** 선택한 신청들을 LOCKED → ORDERED로 전환. Order/OrderItem 자동 누적. */
    @PatchMapping("/book-requests/mark-ordered")
    public ApiResponse<OrderResponse> markOrdered(
        @PathVariable Long clubId,
        @RequestBody MarkRequest req,
        @AuthenticationPrincipal Member member
    ) {
        return ApiResponse.ok(orderService.markOrdered(clubId, member.getId(), req.ids()));
    }

    /** 선택한 신청들을 ORDERED → LOCKED로 되돌림. */
    @PatchMapping("/book-requests/mark-locked")
    public ApiResponse<OrderResponse> markLocked(
        @PathVariable Long clubId,
        @RequestBody MarkRequest req,
        @AuthenticationPrincipal Member member
    ) {
        return ApiResponse.ok(orderService.markLocked(clubId, member.getId(), req.ids()).orElse(null));
    }

    private YearMonth parse(String yearMonth) {
        return (yearMonth == null || yearMonth.isBlank()) ? YearMonth.now() : YearMonth.parse(yearMonth);
    }
}
