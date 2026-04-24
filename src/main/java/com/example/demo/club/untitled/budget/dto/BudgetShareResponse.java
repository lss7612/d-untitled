package com.example.demo.club.untitled.budget.dto;

import com.example.demo.club.untitled.budget.domain.BudgetShare;
import com.example.demo.club.untitled.budget.domain.BudgetShareStatus;
import com.example.demo.user.domain.Member;

import java.time.LocalDateTime;

/**
 * 나눔 이체(BudgetShare) 응답 DTO.
 * 목록·단건 조회 모두 이 한 타입으로 내려감 (배너/수락 대기 리스트/이력 공용).
 */
public record BudgetShareResponse(
    Long id,
    Long clubId,
    String targetMonth,
    Long requesterId,
    String requesterName,
    Long senderId,
    String senderName,
    int amount,
    BudgetShareStatus status,
    String note,
    LocalDateTime createdAt,
    LocalDateTime respondedAt
) {
    public static BudgetShareResponse of(BudgetShare s, Member requester, Member sender) {
        return new BudgetShareResponse(
            s.getId(),
            s.getClubId(),
            s.getTargetMonth(),
            s.getRequesterId(),
            requester != null ? requester.getName() : null,
            s.getSenderId(),
            sender != null ? sender.getName() : null,
            s.getAmount(),
            s.getStatus(),
            s.getNote(),
            s.getCreatedAt(),
            s.getRespondedAt()
        );
    }
}
