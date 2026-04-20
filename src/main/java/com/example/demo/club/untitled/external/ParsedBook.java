package com.example.demo.club.untitled.external;

import java.math.BigDecimal;

public record ParsedBook(
    String title,
    String author,
    String publisher,
    String isbn,
    BigDecimal price,
    String currency,
    String thumbnailUrl,
    String sourceUrl
) {}
