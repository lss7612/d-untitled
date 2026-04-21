package com.example.demo.club.untitled.bootstrap;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * dev 일회성 reset runner.
 * application-local.properties에 `app.reset-orders=true` 설정 시:
 * - order_items / orders 테이블 비우기
 * - BookRequest 중 ORDERED → PENDING으로 복원 + orderId=null
 * 청크 9 점진 누적 도입 전에 만들어둔 stale data 정리용.
 *
 * 사용 후 application-local.properties에서 `app.reset-orders` 제거하거나 false로 변경.
 */
@Slf4j
@Component
@Order(1)
@ConditionalOnProperty(name = "app.reset-orders", havingValue = "true")
public class OrdersCleanupRunner implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            int items = em.createNativeQuery("DELETE FROM order_items").executeUpdate();
            int orders = em.createNativeQuery("DELETE FROM orders").executeUpdate();
            int reverted = em.createNativeQuery(
                "UPDATE book_requests SET status='PENDING', order_id=NULL WHERE status='ORDERED'"
            ).executeUpdate();
            log.warn("[OrdersCleanupRunner] RESET 완료: order_items={}, orders={}, book_requests reverted={}. " +
                "이제 application-local.properties에서 app.reset-orders 제거하세요.",
                items, orders, reverted);
        } catch (Exception e) {
            log.error("[OrdersCleanupRunner] reset 실패: {}", e.getMessage(), e);
        }
    }
}
