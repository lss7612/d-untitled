package com.example.demo.club.untitled.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "book_reports",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_book_report_member_month",
        columnNames = {"club_id", "member_id", "target_month"}
    ),
    indexes = {
        @Index(name = "idx_br_report_member_month", columnList = "member_id, target_month"),
        @Index(name = "idx_br_report_club_month", columnList = "club_id, target_month")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class BookReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신청 도서에서 작성한 경우의 백링크. 다른 책으로 작성한 경우 null. */
    @Column(name = "book_request_id")
    private Long bookRequestId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "target_month", nullable = false, length = 7)
    private String targetMonth;

    /** 도서 정보 스냅샷 (BookRequest를 따라가지 않고 작성 시점 값을 보존) */
    @Column(name = "book_title", nullable = false, length = 300)
    private String bookTitle;

    @Column(name = "book_author", length = 200)
    private String bookAuthor;

    @Column(name = "book_thumbnail_url", length = 500)
    private String bookThumbnailUrl;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreatedDate
    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static BookReport create(
        Long bookRequestId, Long clubId, Long memberId, String targetMonth,
        String bookTitle, String bookAuthor, String bookThumbnailUrl,
        String title, String content
    ) {
        BookReport r = new BookReport();
        r.bookRequestId = bookRequestId;
        r.clubId = clubId;
        r.memberId = memberId;
        r.targetMonth = targetMonth;
        r.bookTitle = bookTitle;
        r.bookAuthor = bookAuthor;
        r.bookThumbnailUrl = bookThumbnailUrl;
        r.title = title;
        r.content = content;
        return r;
    }

    public void edit(
        Long bookRequestId, String bookTitle, String bookAuthor, String bookThumbnailUrl,
        String title, String content
    ) {
        this.bookRequestId = bookRequestId;
        this.bookTitle = bookTitle;
        this.bookAuthor = bookAuthor;
        this.bookThumbnailUrl = bookThumbnailUrl;
        this.title = title;
        this.content = content;
    }
}
