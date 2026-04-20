package com.example.demo.club.untitled.dto;

import com.example.demo.club.untitled.domain.BookCategory;

public record BookRequestCreateRequest(
    String url,
    BookCategory category
) {}
