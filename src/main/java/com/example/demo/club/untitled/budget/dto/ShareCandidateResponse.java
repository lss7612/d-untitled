package com.example.demo.club.untitled.budget.dto;

/**
 * 나눔 신청 화면의 후보 멤버 엔트리.
 * 본인 제외한 클럽 ACTIVE 멤버와 이체 반영된 잔여 한도(remaining) 를 함께 내려 UI 에서 바로 "여유 있는 사람" 을 고를 수 있게 한다.
 */
public record ShareCandidateResponse(
    Long memberId,
    String name,
    String email,
    int remaining
) {
}
