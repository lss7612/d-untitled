package com.example.demo.club.untitled.controller;

import com.example.demo.club.untitled.dto.BookResponse;
import com.example.demo.club.untitled.service.BookCatalogService;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.user.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 회원: 보유 책 카탈로그 검색. */
@RestController
@RequestMapping("/api/v1/clubs/{clubId}")
@RequiredArgsConstructor
public class BookController {

    private final BookCatalogService bookCatalogService;

    @GetMapping("/books")
    public ApiResponse<List<BookResponse>> search(
        @PathVariable Long clubId,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String author,
        @AuthenticationPrincipal Member member
    ) {
        return ApiResponse.ok(bookCatalogService.search(clubId, member.getId(), title, author));
    }
}
