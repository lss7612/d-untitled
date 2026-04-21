package com.example.demo.club.untitled.domain;

public enum OrderStatus {
    DRAFT,      // 합산 주문서 생성됨, 아직 알라딘 주문 전
    ORDERED,    // 알라딘에 실제 주문 완료
    SHIPPING,   // 배송 중
    ARRIVED,    // 도착
    CANCELLED   // 취소
}
