package com.example.demo.club.untitled.repository;

import com.example.demo.club.untitled.domain.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * 클럽 카탈로그에서 정규화된 (title, author) 로 책 조회.
     * {@code normalized*} 컬럼은 {@link Book} 의 @PrePersist / @PreUpdate 로 유지된다.
     */
    Optional<Book> findByClubIdAndNormalizedTitleAndNormalizedAuthor(
        Long clubId, String normalizedTitle, String normalizedAuthor);

    List<Book> findAllByClubIdOrderByCreatedAtDesc(Long clubId);

    /**
     * 보유 책 검색. 제목/저자에 대해 정규화된 값 기준 contains 매칭.
     * 파라미터가 null 이면 해당 조건 skip.
     * LIKE 패턴의 {@code %}, {@code _}, {@code \} 이스케이프는 서비스 계층에서 처리해 전달해야 한다.
     *
     * 주: {@code LIKE '%x%'} 는 선행 와일드카드라 B-tree 인덱스를 못 탄다 (풀 스캔).
     * 클럽당 수천 건 규모까지는 체감 부담 없음. 5,000 건 이상이면 FULLTEXT ngram 고려.
     */
    @Query("""
        select b from Book b
        where b.clubId = :clubId
          and (:normTitle is null or b.normalizedTitle like :normTitle escape '\\')
          and (:normAuthor is null or b.normalizedAuthor like :normAuthor escape '\\')
        order by b.createdAt desc
    """)
    List<Book> searchInClub(
        @Param("clubId") Long clubId,
        @Param("normTitle") String normTitle,
        @Param("normAuthor") String normAuthor,
        Pageable pageable
    );

    /** 현재 제한풀기(exemption)가 승인된 상태인 책 목록 — AdminExemptBooksPage 용. */
    List<Book> findAllByClubIdAndExemptionGrantedAtIsNotNullOrderByExemptionGrantedAtDesc(Long clubId);
}
