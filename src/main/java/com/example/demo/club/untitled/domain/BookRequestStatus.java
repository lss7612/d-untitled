package com.example.demo.club.untitled.domain;

public enum BookRequestStatus {
    PENDING("신청 대기"),     // 신청 접수, 수정/취소 가능
    ORDERED("신청 완료"),      // 관리자가 신청완료 처리 (PENDING → ORDERED)
    ARRIVED("도착"),           // 인포 도착
    RECEIVED("수령 완료");     // 회원 수령 완료

    private final String label;

    BookRequestStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
