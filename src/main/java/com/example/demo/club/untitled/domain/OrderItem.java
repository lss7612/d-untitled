package com.example.demo.club.untitled.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "order_items",
    indexes = @Index(name = "idx_oi_order", columnList = "order_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 30)
    private String isbn;

    /** 알라딘 K-CODE. 카트 URL 생성에 사용. nullable (소스에 코드 없는 경우). */
    @Column(name = "aladin_item_code", length = 20)
    private String aladinItemCode;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 200)
    private String author;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer subtotal;

    public static OrderItem create(
        Long orderId, String isbn, String aladinItemCode,
        String title, String author, int unitPrice, int quantity
    ) {
        OrderItem oi = new OrderItem();
        oi.orderId = orderId;
        oi.isbn = isbn;
        oi.aladinItemCode = aladinItemCode;
        oi.title = title;
        oi.author = author;
        oi.unitPrice = unitPrice;
        oi.quantity = quantity;
        oi.subtotal = unitPrice * quantity;
        return oi;
    }

    public void increase(int delta) {
        this.quantity += delta;
        this.subtotal = this.unitPrice * this.quantity;
    }

    public void decrease(int delta) {
        this.quantity -= delta;
        if (this.quantity < 0) this.quantity = 0;
        this.subtotal = this.unitPrice * this.quantity;
    }
}
