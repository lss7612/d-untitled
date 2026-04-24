package com.example.demo.club.untitled.controller;

import com.example.demo.club.untitled.dto.BookExemptionResponse;
import com.example.demo.club.untitled.dto.CreateBookExemptionRequest;
import com.example.demo.club.untitled.service.BookExemptionService;
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
            throw new com.example.demo.common.exception.BusinessException("bookId 가 필요합니다.", 400);
        }
        return ApiResponse.ok(bookExemptionService.request(clubId, req.bookId(), req.reason(), member));
    }
}
