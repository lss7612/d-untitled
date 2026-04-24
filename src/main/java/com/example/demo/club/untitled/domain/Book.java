package com.example.demo.club.untitled.domain;

import com.example.demo.club.untitled.util.BookNames;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 동호회가 이미 보유한 책 카탈로그.
 *
 * <p>회원 책 신청 시 {@code (clubId, normalizedTitle, normalizedAuthor)} 로 중복을 판정한다.
 * 같은 책이라도 {@link #exemptionGrantedAt} 가 세팅되어 있으면 중복 체크에서 제외된다
 * (관리자가 제한풀기 신청을 승인한 책).
 */
@Entity
@Table(
    name = "books",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_books_club_title_author",
        columnNames = {"club_id", "normalized_title", "normalized_author"}
    ),
    indexes = {
        @Index(name = "idx_books_club", columnList = "club_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 200)
    private String author;

    @Column(name = "normalized_title", nullable = false, length = 300)
    private String normalizedTitle;

    @Column(name = "normalized_author", nullable = false, length = 200)
    private String normalizedAuthor;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(30)")
    private BookCategory category;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    /** 구매 시점 KRW 가격 (시드 데이터의 "지원요청액"). nullable — 시트에 값 없을 수 있음. */
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    /** 제한풀기 승인 시각. NULL = 중복 체크에서 걸린다. 값이 있으면 중복 체크 우회. */
    @Column(name = "exemption_granted_at")
    private LocalDateTime exemptionGrantedAt;

    /** 동호회가 실제로 보유 중인 이 책의 권수. BookRequest 가 RECEIVED 로 전이될 때 증가. */
    @Column(nullable = false, columnDefinition = "INT NOT NULL DEFAULT 1")
    private int copies = 1;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void syncNormalized() {
        this.normalizedTitle = BookNames.normalize(title);
        this.normalizedAuthor = BookNames.normalize(author);
    }

    public static Book of(Long clubId, String title, String author, BookCategory category,
                          BigDecimal price, String sourceUrl, String thumbnailUrl) {
        Book b = new Book();
        b.clubId = clubId;
        b.title = title;
        b.author = author;
        b.category = category;
        b.price = price;
        b.sourceUrl = sourceUrl;
        b.thumbnailUrl = thumbnailUrl;
        // normalized_* 는 @PrePersist 에서 채워짐
        return b;
    }

    /**
     * 아직 아무도 수령하지 않은 "카탈로그 선반영" 용 팩토리. 제한풀기(exemption) 플로우에서
     * book_request 충돌이나 관리자 URL 해제 시 쓴다. RECEIVED 전이에서 {@link #incrementCopies()} 로 1 이상 된다.
     */
    public static Book ofCatalogPreentry(Long clubId, String title, String author,
                                          BigDecimal price, String sourceUrl, String thumbnailUrl) {
        Book b = new Book();
        b.clubId = clubId;
        b.title = title;
        b.author = author;
        b.price = price;
        b.sourceUrl = sourceUrl;
        b.thumbnailUrl = thumbnailUrl;
        b.copies = 0;
        return b;
    }

    /** 제한풀기 신청이 승인되면 호출 — 이후 중복 체크에서 이 책은 제외된다. */
    public void grantExemption() {
        this.exemptionGrantedAt = LocalDateTime.now();
    }

    /** 관리자가 제한을 다시 걸 때 호출 — 이후 이 책은 다시 중복 체크 대상이 된다. */
    public void revokeExemption() {
        this.exemptionGrantedAt = null;
    }

    public boolean isExempt() {
        return exemptionGrantedAt != null;
    }

    /** BookRequest 가 RECEIVED 로 전이될 때 호출 — 보유 권수 +1. */
    public void incrementCopies() {
        this.copies += 1;
    }
}
