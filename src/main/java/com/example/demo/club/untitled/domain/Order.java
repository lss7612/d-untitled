package com.example.demo.club.untitled.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "orders",
    indexes = @Index(name = "idx_order_club_month", columnList = "club_id, target_month")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "target_month", nullable = false, length = 7)
    private String targetMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ordered_at")
    private LocalDateTime orderedAt;

    public static Order create(Long clubId, String targetMonth, Long createdBy, int totalAmount, int totalQuantity) {
        Order o = new Order();
        o.clubId = clubId;
        o.targetMonth = targetMonth;
        o.status = OrderStatus.DRAFT;
        o.totalAmount = totalAmount;
        o.totalQuantity = totalQuantity;
        o.createdBy = createdBy;
        return o;
    }

    public void markOrdered() {
        this.status = OrderStatus.ORDERED;
        this.orderedAt = LocalDateTime.now();
    }

    public void changeStatus(OrderStatus status) {
        this.status = status;
    }

    public void updateTotal(int totalAmount, int totalQuantity) {
        this.totalAmount = totalAmount;
        this.totalQuantity = totalQuantity;
    }
}
