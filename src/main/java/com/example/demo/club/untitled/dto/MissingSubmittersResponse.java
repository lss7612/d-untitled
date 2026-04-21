package com.example.demo.club.untitled.dto;

import java.time.LocalDate;
import java.util.List;

public record MissingSubmittersResponse(
    String targetMonth,
    LocalDate deadline,
    int totalMembers,
    int submittedCount,
    List<MissingSubmitter> missing
) {
    public record MissingSubmitter(Long memberId, String memberName, String memberEmail) {}
}
