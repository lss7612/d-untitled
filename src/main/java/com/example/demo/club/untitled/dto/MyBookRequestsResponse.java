package com.example.demo.club.untitled.dto;

import java.util.List;

public record MyBookRequestsResponse(
    String targetMonth,
    int budgetLimit,
    int budgetUsed,
    int budgetRemaining,
    boolean locked,
    List<BookRequestResponse> requests
) {}
