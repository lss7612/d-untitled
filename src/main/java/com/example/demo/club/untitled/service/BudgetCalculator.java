package com.example.demo.club.untitled.service;

import com.example.demo.club.untitled.domain.BookRequest;
import com.example.demo.user.domain.Member;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;

/**
 * 가입월(`Member.createdAt`의 YearMonth) 기준 예산 한도 계산.
 * - 가입월: 30,000원
 * - 그 다음 월부터: 35,000원
 */
@Component
public class BudgetCalculator {

    public static final int FIRST_MONTH_LIMIT = 30_000;
    public static final int REGULAR_LIMIT = 35_000;

    public int limitFor(Member member, YearMonth targetMonth) {
        YearMonth joinMonth = YearMonth.from(member.getCreatedAt());
        return joinMonth.equals(targetMonth) ? FIRST_MONTH_LIMIT : REGULAR_LIMIT;
    }

    public int sumUsed(List<BookRequest> requestsThisMonth) {
        return requestsThisMonth.stream()
            .mapToInt(BookRequest::getPrice)
            .sum();
    }

    public int remaining(Member member, YearMonth targetMonth, List<BookRequest> requestsThisMonth) {
        return limitFor(member, targetMonth) - sumUsed(requestsThisMonth);
    }
}
