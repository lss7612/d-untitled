package com.example.demo.club.untitled.bootstrap;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * dev 전용 일회성 스키마 보정.
 * 청크 7에서 만든 book_reports 테이블이 (book_request_id NOT NULL UNIQUE)였는데,
 * 청크 7-수정에서 nullable + 신규 unique(club_id, member_id, target_month)로 변경됨.
 * Hibernate update 모드는 NOT NULL 해제와 unique 제약 변경을 자동으로 안 함.
 *
 * 데이터가 거의 없는 시점이라 테이블을 통째로 drop → Hibernate가 재생성.
 * 운영에 가까워지면 Flyway/Liquibase로 대체할 것.
 */
@Slf4j
@Component
@Order(0)
public class BookReportSchemaFixer implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(String... args) {
        boolean tableExists = false;
        boolean hasBookTitle = false;
        boolean hasRating = false;
        try {
            tableExists = ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME)='BOOK_REPORTS'"
            ).getSingleResult()).intValue() > 0;
        } catch (Exception ignored) {}

        if (!tableExists) {
            log.info("[BookReportSchemaFixer] book_reports 테이블 없음 → Hibernate 신규 생성");
            return;
        }

        try {
            hasBookTitle = ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE UPPER(TABLE_NAME)='BOOK_REPORTS' AND UPPER(COLUMN_NAME)='BOOK_TITLE'"
            ).getSingleResult()).intValue() > 0;
            hasRating = ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE UPPER(TABLE_NAME)='BOOK_REPORTS' AND UPPER(COLUMN_NAME)='RATING'"
            ).getSingleResult()).intValue() > 0;
        } catch (Exception ignored) {}

        // 1) 구 스키마(book_title 없음) → 통째로 drop
        if (!hasBookTitle) {
            try {
                em.createNativeQuery("DROP TABLE IF EXISTS book_reports CASCADE").executeUpdate();
                log.info("[BookReportSchemaFixer] 구 스키마 감지 → drop (Hibernate가 신 스키마로 재생성)");
            } catch (Exception e) {
                log.warn("[BookReportSchemaFixer] drop 실패: {}", e.getMessage());
            }
            return;
        }

        // 2) book_title은 있는데 rating이 남아있으면 컬럼만 drop
        if (hasRating) {
            try {
                em.createNativeQuery("ALTER TABLE book_reports DROP COLUMN IF EXISTS rating").executeUpdate();
                log.info("[BookReportSchemaFixer] rating 컬럼 drop 완료");
            } catch (Exception e) {
                log.warn("[BookReportSchemaFixer] rating drop 실패: {}", e.getMessage());
            }
        }

        // 3) book_request_id NOT NULL → NULL 허용 (구 UNIQUE 제약 잔재)
        try {
            em.createNativeQuery("ALTER TABLE book_reports ALTER COLUMN book_request_id SET NULL").executeUpdate();
            log.info("[BookReportSchemaFixer] book_request_id NULL 허용 완료");
        } catch (Exception e) {
            log.warn("[BookReportSchemaFixer] book_request_id NULL alter 실패 (이미 nullable일 수 있음): {}", e.getMessage());
        }
    }
}
