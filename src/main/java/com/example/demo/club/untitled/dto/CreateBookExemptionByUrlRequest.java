package com.example.demo.club.untitled.dto;

/** 회원: 알라딘 URL 기반 제한풀기 신청 (book_request 충돌 케이스 대응). */
public record CreateBookExemptionByUrlRequest(String url, String reason) {}
