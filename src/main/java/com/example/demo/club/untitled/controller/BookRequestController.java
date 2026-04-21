package com.example.demo.club.untitled.controller;

import com.example.demo.club.untitled.domain.BookCategory;
import com.example.demo.club.untitled.dto.*;
import com.example.demo.club.untitled.service.BookRequestService;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.user.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class BookRequestController {

    private final BookRequestService bookRequestService;

    @PostMapping("/books/parse-url")
    public ApiResponse<ParsedBookResponse> parseUrl(
        @PathVariable Long clubId,
        @RequestBody ParseUrlRequest req,
        @AuthenticationPrincipal Member member
    ) {
        return ApiResponse.ok(bookRequestService.parseUrl(clubId, member.getId(), req));
    }

    @GetMapping("/books/categories")
    public ApiResponse<List<BookCategory>> categories(@PathVariable Long clubId) {
        return ApiResponse.ok(bookRequestService.categories());
    }

    @GetMapping("/book-requests")
    public ApiResponse<List<BookRequestResponse>> list(
        @PathVariable Long clubId,
        @RequestParam(required = false) String yearMonth,
        @AuthenticationPrincipal Member member
    ) {
        YearMonth ym = (yearMonth == null || yearMonth.isBlank()) ? YearMonth.now() : YearMonth.parse(yearMonth);
        return ApiResponse.ok(bookRequestService.findAllByMonth(clubId, member.getId(), ym));
    }

    @GetMapping("/book-requests/my")
    public ApiResponse<MyBookRequestsResponse> mine(
        @PathVariable Long clubId,
        @RequestParam(required = false) String yearMonth,
        @AuthenticationPrincipal Member member
    ) {
        YearMonth ym = (yearMonth == null || yearMonth.isBlank()) ? YearMonth.now() : YearMonth.parse(yearMonth);
        return ApiResponse.ok(bookRequestService.findMine(clubId, member.getId(), ym));
    }

    @PostMapping("/book-requests")
    public ApiResponse<BookRequestResponse> create(
        @PathVariable Long clubId,
        @RequestBody BookRequestCreateRequest req,
        @AuthenticationPrincipal Member member
    ) {
        return ApiResponse.ok(bookRequestService.create(clubId, member.getId(), req));
    }

    @PatchMapping("/book-requests/{id}")
    public ApiResponse<BookRequestResponse> update(
        @PathVariable Long clubId,
        @PathVariable Long id,
        @RequestBody BookRequestUpdateRequest req,
        @AuthenticationPrincipal Member member
    ) {
        return ApiResponse.ok(bookRequestService.update(clubId, member.getId(), id, req));
    }

    @DeleteMapping("/book-requests/{id}")
    public ApiResponse<Void> delete(
        @PathVariable Long clubId,
        @PathVariable Long id,
        @AuthenticationPrincipal Member member
    ) {
        bookRequestService.delete(clubId, member.getId(), id);
        return ApiResponse.ok(null);
    }

    /** 회원: 본인의 ARRIVED 책들을 RECEIVED로 처리. */
    @PatchMapping("/book-requests/mark-received")
    public ApiResponse<MarkReceivedResult> markReceived(
        @PathVariable Long clubId,
        @RequestBody MarkRequest req,
        @AuthenticationPrincipal Member member
    ) {
        int count = bookRequestService.markReceived(clubId, member.getId(), req.ids());
        return ApiResponse.ok(new MarkReceivedResult(count));
    }

    public record MarkReceivedResult(int affected) {}
}
