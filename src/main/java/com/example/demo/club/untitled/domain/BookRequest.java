package com.example.demo.club.untitled.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(
    name = "book_requests",
    indexes = {
        @Index(name = "idx_br_member_month", columnList = "member_id, target_month"),
        @Index(name = "idx_br_club_month", columnList = "club_id, target_month")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class BookRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 200)
    private String author;

    @Column(length = 100)
    private String publisher;

    @Column(nullable = false, length = 30)
    private String isbn;

    /**
     * KRW 환산 가격. originalPrice * exchangeRate.
     */
    @Column(nullable = false)
    private Integer price;

    @Column(name = "original_price", precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(nullable = false, length = 5)
    private String currency;

    /**
     * 신청 시점 환율. 국내서는 1.0.
     */
    @Column(name = "exchange_rate", precision = 12, scale = 4)
    private BigDecimal exchangeRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookCategory category;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    /** 알라딘 내부 도서 코드(K-CODE). 장바구니 URL 생성에 사용. nullable (알라딘이 코드 노출 안 한 도서). */
    @Column(name = "aladin_item_code", length = 20)
    private String aladinItemCode;

    /** 합산 주문서 ID. 신청이 주문서에 묶이면 세팅. nullable. */
    @Column(name = "order_id")
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookRequestStatus status;

    @Column(name = "target_month", nullable = false, length = 7)
    private String targetMonth;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /** 도착 처리 시각 (ARRIVED 진입 시 세팅, ARRIVED→ORDERED 되돌리면 null). */
    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    /** 수령 처리 시각 (RECEIVED 진입 시 세팅). */
    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    public static BookRequest create(
        Long clubId, Long memberId,
        String title, String author, String publisher, String isbn,
        Integer price, BigDecimal originalPrice, String currency, BigDecimal exchangeRate,
        BookCategory category, String sourceUrl, String thumbnailUrl, String aladinItemCode,
        YearMonth targetMonth
    ) {
        BookRequest br = new BookRequest();
        br.clubId = clubId;
        br.memberId = memberId;
        br.title = title;
        br.author = author;
        br.publisher = publisher;
        br.isbn = isbn;
        br.price = price;
        br.originalPrice = originalPrice;
        br.currency = currency;
        br.exchangeRate = exchangeRate;
        br.category = category;
        br.sourceUrl = sourceUrl;
        br.thumbnailUrl = thumbnailUrl;
        br.aladinItemCode = aladinItemCode;
        br.status = BookRequestStatus.PENDING;
        br.targetMonth = targetMonth.toString();
        return br;
    }

    public void assignToOrder(Long orderId) {
        this.orderId = orderId;
        this.status = BookRequestStatus.ORDERED;
    }

    public void revertToLocked() {
        this.orderId = null;
        this.status = BookRequestStatus.LOCKED;
    }

    public void markArrived() {
        this.status = BookRequestStatus.ARRIVED;
        this.arrivedAt = LocalDateTime.now();
    }

    public void markUnarrived() {
        this.status = BookRequestStatus.ORDERED;
        this.arrivedAt = null;
    }

    public void markReceived() {
        this.status = BookRequestStatus.RECEIVED;
        this.receivedAt = LocalDateTime.now();
    }

    public void edit(BookCategory category) {
        if (status != BookRequestStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태가 아닙니다.");
        }
        this.category = category;
    }

    public void changeStatus(BookRequestStatus status) {
        this.status = status;
    }

    public boolean isEditable() {
        return status == BookRequestStatus.PENDING;
    }

    public YearMonth getTargetYearMonth() {
        return YearMonth.parse(targetMonth);
    }
}
