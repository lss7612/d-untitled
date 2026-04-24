package com.example.demo.club.untitled.controller;

import com.example.demo.club.untitled.dto.BookExemptionResponse;
import com.example.demo.club.untitled.dto.CreateBookExemptionByUrlRequest;
import com.example.demo.club.untitled.dto.CreateBookExemptionRequest;
import com.example.demo.club.untitled.service.BookExemptionService;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.user.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 회원: 중복 책 에러 시 "제한풀기" 신청을 보내는 엔드포인트. */
@RestController
@RequestMapping("/api/v1/clubs/{clubId}/book-exemptions")
@RequiredArgsConstructor
public class BookExemptionController {

    private final BookExemptionService bookExemptionService;

    @PostMapping
    public ApiResponse<BookExemptionResponse> create(
        @PathVariable Long clubId,
        @RequestBody CreateBookExemptionRequest req,
        @AuthenticationPrincipal Member member
    ) {
        if (req == null || req.bookId() == null) {
            throw new BusinessException("bookId 가 필요합니다.", 400);
        }
        return ApiResponse.ok(bookExemptionService.request(clubId, req.bookId(), req.reason(), member));
    }

    /**
     * 카탈로그에 없는 책(다른 회원의 동월 PENDING 과 충돌 등) 에 대한 제한풀기 신청.
     * 신청 폼에 입력한 알라딘 URL 을 그대로 재전송한다.
     */
    @PostMapping("/by-url")
    public ApiResponse<BookExemptionResponse> createByUrl(
        @PathVariable Long clubId,
        @RequestBody CreateBookExemptionByUrlRequest req,
        @AuthenticationPrincipal Member member
    ) {
        if (req == null || req.url() == null || req.url().isBlank()) {
            throw new BusinessException("알라딘 URL 이 필요합니다.", 400);
        }
        return ApiResponse.ok(bookExemptionService.requestByUrl(clubId, req.url(), req.reason(), member));
    }
}
