package com.example.demo.club.untitled.domain;

public enum BookRequestStatus {
    PENDING,    // 신청 접수, 수정/취소 가능
    LOCKED,     // 신청 마감 잠금 (관리자가 lock 처리한 후)
    ORDERED,    // 주문 처리됨
    SHIPPING,   // 배송 중
    ARRIVED,    // 인포 도착
    RECEIVED    // 회원 수령 완료
}
