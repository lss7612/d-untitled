package com.example.demo.club.untitled.budget.dto;

public record AdjustBudgetRequest(
    int baseLimit,
    String reason
) {}
