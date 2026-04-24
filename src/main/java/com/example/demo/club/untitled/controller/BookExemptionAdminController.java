package com.example.demo.club.untitled.controller;

import com.example.demo.club.untitled.dto.BookExemptionResponse;
import com.example.demo.club.untitled.dto.BookResponse;
import com.example.demo.club.untitled.service.BookExemptionService;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.user.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 관리자: 제한풀기 신청 목록/승인/거절 + 제한풀기된 책 목록/재적용(revoke). */
@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}")
@RequiredArgsConstructor
public class BookExemptionAdminController {

    private final BookExemptionService bookExemptionService;

    @GetMapping("/book-exemptions")
    public ApiResponse<List<BookExemptionResponse>> listPending(
        @PathVariable Long clubId,
        @AuthenticationPrincipal Member member
    ) {
        return ApiResponse.ok(bookExemptionService.listPending(clubId, member));
    }

    @PostMapping("/book-exemptions/{id}/approve")
    public ApiResponse<BookExemptionResponse> approve(
        @PathVariable Long clubId,
        @PathVariable Long id,
        @AuthenticationPrincipal Member member
    ) {
        return ApiResponse.ok(bookExemptionService.approve(clubId, id, member));
    }

    @PostMapping("/book-exemptions/{id}/reject")
    public ApiResponse<BookExemptionResponse> reject(
        @PathVariable Long clubId,
        @PathVariable Long id,
        @AuthenticationPrincipal Member member
    ) {
        return ApiResponse.ok(bookExemptionService.reject(clubId, id, member));
    }

    /** 제한풀기(exemption)가 승인되어 있는 책 목록. */
    @GetMapping("/books/exempt")
    public ApiResponse<List<BookResponse>> listExemptBooks(
        @PathVariable Long clubId,
        @AuthenticationPrincipal Member member
    ) {
        return ApiResponse.ok(bookExemptionService.listExempt(clubId, member));
    }

    /** 책의 exemption 을 제거하여 다시 중복 제한 상태로 되돌린다 (idempotent). */
    @DeleteMapping("/books/{bookId}/exemption")
    public ApiResponse<BookResponse> revokeExemption(
        @PathVariable Long clubId,
        @PathVariable Long bookId,
        @AuthenticationPrincipal Member member
    ) {
        return ApiResponse.ok(bookExemptionService.revoke(clubId, bookId, member));
    }
}
