package com.example.demo.club.untitled.dto;

import com.example.demo.club.untitled.external.ParsedBook;

import java.math.BigDecimal;

public record ParsedBookResponse(
    String title,
    String author,
    String publisher,
    String isbn,
    BigDecimal originalPrice,
    String currency,
    BigDecimal exchangeRate,
    Integer priceKrw,
    String thumbnailUrl,
    String sourceUrl,
    String aladinItemCode
) {
    public static ParsedBookResponse of(ParsedBook book, BigDecimal exchangeRate, int priceKrw) {
        return new ParsedBookResponse(
            book.title(),
            book.author(),
            book.publisher(),
            book.isbn(),
            book.price(),
            book.currency(),
            exchangeRate,
            priceKrw,
            book.thumbnailUrl(),
            book.sourceUrl(),
            book.aladinItemCode()
        );
    }
}
