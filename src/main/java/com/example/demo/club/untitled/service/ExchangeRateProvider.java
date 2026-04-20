package com.example.demo.club.untitled.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 임시 환율 테이블. 신청 시점에 동결할 환율 1개를 제공.
 * Phase 2 후반에 실시간 환율 API 연동으로 정교화 예정.
 */
@Component
public class ExchangeRateProvider {

    private static final BigDecimal ONE = BigDecimal.ONE;

    private static final Map<String, BigDecimal> RATES = Map.of(
        "KRW", ONE,
        "USD", new BigDecimal("1380.00"),
        "JPY", new BigDecimal("9.20"),
        "EUR", new BigDecimal("1500.00"),
        "GBP", new BigDecimal("1750.00"),
        "CNY", new BigDecimal("190.00")
    );

    public BigDecimal rateFor(String currency) {
        if (currency == null) return ONE;
        return RATES.getOrDefault(currency.toUpperCase(), ONE);
    }
}
