package com.example.demo.club.untitled.bootstrap;

import com.example.demo.club.domain.Club;
import com.example.demo.club.repository.ClubRepository;
import com.example.demo.club.service.ClubMembershipService;
import com.example.demo.club.untitled.domain.BookCategory;
import com.example.demo.club.untitled.domain.BookReport;
import com.example.demo.club.untitled.domain.BookRequest;
import com.example.demo.club.untitled.domain.BookRequestStatus;
import com.example.demo.club.untitled.domain.OrderItem;
import com.example.demo.club.untitled.external.AladinApiClient;
import com.example.demo.club.untitled.external.ParsedBook;
import com.example.demo.club.untitled.repository.BookReportRepository;
import com.example.demo.club.untitled.repository.BookRequestRepository;
import com.example.demo.club.untitled.repository.OrderItemRepository;
import com.example.demo.club.untitled.repository.OrderRepository;
import com.example.demo.club.untitled.service.ExchangeRateProvider;
import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * dev 더미 데이터 시드.
 * application-local.properties에 `app.seed-dummies=true` 설정 시 1회 실행.
 * 사용 후 false로 끄세요.
 */
@Slf4j
@Component
@Order(3)
@ConditionalOnProperty(name = "app.seed-dummies", havingValue = "true")
@RequiredArgsConstructor
public class DummyDataSeeder implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final ClubRepository clubRepository;
    private final ClubMembershipService clubMembershipService;
    private final BookRequestRepository bookRequestRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookReportRepository bookReportRepository;
    private final AladinApiClient aladinApiClient;
    private final ExchangeRateProvider exchangeRateProvider;
    private final TransactionTemplate transactionTemplate;

    private static final String[][] DUMMY_MEMBERS = {
        {"김민수", "kim.minsu@example.com"},
        {"이지영", "lee.jiyoung@example.com"},
        {"박지훈", "park.jihoon@example.com"},
        {"최서연", "choi.seoyeon@example.com"},
        {"정현우", "jung.hyunwoo@example.com"},
        {"강유진", "kang.yujin@example.com"},
        {"조태웅", "jo.taewoong@example.com"},
        {"윤하늘", "yoon.haneul@example.com"},
    };

    private static final String[] BOOK_URLS = {
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=270454373",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=365665217",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=389894398",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=390907740",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=388169547",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=388092141",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=376765918",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=390196640",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=385481121",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=390659734",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=386947012",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=390661137",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=390779612",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=388149167",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=388509769",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=378969369",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=378969617",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=385370551",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=387930371",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=25843736",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=329596",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=390776539",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=388727411",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=390779373",
        "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=379450520",
    };

    private static final BookCategory[] CATEGORY_ROTATION = BookCategory.values();

    @Override
    public void run(String... args) {
        log.info("[DummySeeder] 시작");
        Club muje = clubRepository.findByName(ClubMembershipService.DEFAULT_CLUB_NAME).orElse(null);
        if (muje == null) {
            log.warn("[DummySeeder] 무제 동호회가 없습니다. ClubBootstrap 후에 다시 실행하세요.");
            return;
        }

        // 단계별로 별도 트랜잭션 — 한 단계 실패가 전체 rollback으로 이어지지 않도록.
        List<Member> dummies = txInvoke("seedMembers", () -> seedMembers());
        if (dummies == null) dummies = List.of();
        final List<Member> finalDummies = dummies;

        List<BookRequest> books = txInvoke("seedBookRequests", () -> seedBookRequests(muje, finalDummies));
        if (books == null) books = List.of();
        final List<BookRequest> finalBooks = books;

        txInvoke("seedOrderEntity", () -> { seedOrderEntity(muje, finalBooks); return null; });
        txInvoke("seedBookReports", () -> { seedBookReports(muje, finalDummies, finalBooks); return null; });

        log.info("[DummySeeder] 완료. application-local.properties에서 app.seed-dummies=false로 변경하세요.");
    }

    private <T> T txInvoke(String label, java.util.function.Supplier<T> action) {
        try {
            return transactionTemplate.execute(status -> {
                try {
                    return action.get();
                } catch (RuntimeException e) {
                    log.error("[DummySeeder] {} 실패: {}", label, e.getMessage(), e);
                    status.setRollbackOnly();
                    return null;
                }
            });
        } catch (Exception e) {
            log.error("[DummySeeder] {} 트랜잭션 자체 오류: {}", label, e.getMessage(), e);
            return null;
        }
    }

    private List<Member> seedMembers() {
        List<Member> created = new ArrayList<>();
        for (String[] m : DUMMY_MEMBERS) {
            String name = m[0];
            String email = m[1];
            Member existing = memberRepository.findByEmail(email).orElse(null);
            if (existing != null) {
                created.add(existing);
                continue;
            }
            Member member = Member.create(email, name, null);
            member.verifyEmail();
            Member saved = memberRepository.save(member);
            clubMembershipService.autoEnroll(saved);
            created.add(saved);
            log.info("[DummySeeder] 회원 생성: {} ({})", name, email);
        }
        return created;
    }

    private List<BookRequest> seedBookRequests(Club muje, List<Member> members) {
        if (members.isEmpty()) {
            log.warn("[DummySeeder] 회원이 없어 책 시드 skip");
            return List.of();
        }
        YearMonth thisMonth = YearMonth.now();
        List<BookRequest> result = new ArrayList<>();

        for (int i = 0; i < BOOK_URLS.length; i++) {
            String url = BOOK_URLS[i];
            Member assignee = members.get(i % members.size());
            BookCategory category = CATEGORY_ROTATION[i % CATEGORY_ROTATION.length];

            ParsedBook parsed;
            try {
                parsed = aladinApiClient.parse(url);
            } catch (Exception e) {
                log.warn("[DummySeeder] 알라딘 파싱 실패 (skip): {} reason={}", url, e.getMessage());
                continue;
            }

            if (bookRequestRepository.findByMemberIdAndClubIdAndTargetMonthAndIsbn(
                    assignee.getId(), muje.getId(), thisMonth.toString(), parsed.isbn()).isPresent()) {
                log.info("[DummySeeder] 책 중복 (skip): {} - {}", assignee.getName(), parsed.title());
                continue;
            }

            BigDecimal rate = exchangeRateProvider.rateFor(parsed.currency());
            int priceKrw = parsed.price().multiply(rate).setScale(0, RoundingMode.HALF_UP).intValue();

            BookRequest entity = BookRequest.create(
                muje.getId(), assignee.getId(),
                parsed.title(), parsed.author(), parsed.publisher(), parsed.isbn(),
                priceKrw, parsed.price(), parsed.currency(), rate,
                category, parsed.sourceUrl(), parsed.thumbnailUrl(), parsed.aladinItemCode(),
                thisMonth
            );

            BookRequestStatus status = statusFor(i);
            applyStatus(entity, status);

            BookRequest saved = bookRequestRepository.save(entity);
            result.add(saved);
            log.info("[DummySeeder] 책: [{}] {} → {} ({})", i, assignee.getName(), parsed.title(), status);
        }
        return result;
    }

    private BookRequestStatus statusFor(int index) {
        if (index < 5) return BookRequestStatus.PENDING;
        if (index < 10) return BookRequestStatus.LOCKED;
        if (index < 18) return BookRequestStatus.ORDERED;
        if (index < 22) return BookRequestStatus.ARRIVED;
        return BookRequestStatus.RECEIVED;
    }

    private void applyStatus(BookRequest br, BookRequestStatus status) {
        switch (status) {
            case PENDING -> { /* default */ }
            case LOCKED -> br.changeStatus(BookRequestStatus.LOCKED);
            case ORDERED -> {
                br.changeStatus(BookRequestStatus.LOCKED);
                br.changeStatus(BookRequestStatus.ORDERED);
            }
            case ARRIVED -> {
                br.changeStatus(BookRequestStatus.LOCKED);
                br.changeStatus(BookRequestStatus.ORDERED);
                br.markArrived();
            }
            case RECEIVED -> {
                br.changeStatus(BookRequestStatus.LOCKED);
                br.changeStatus(BookRequestStatus.ORDERED);
                br.markArrived();
                br.markReceived();
            }
            default -> { /* SHIPPING 등 unused */ }
        }
    }

    private void seedOrderEntity(Club muje, List<BookRequest> books) {
        List<BookRequest> orderedish = books.stream()
            .filter(br -> br.getStatus() == BookRequestStatus.ORDERED
                       || br.getStatus() == BookRequestStatus.ARRIVED
                       || br.getStatus() == BookRequestStatus.RECEIVED)
            .toList();
        if (orderedish.isEmpty()) return;

        YearMonth thisMonth = YearMonth.now();
        Optional<com.example.demo.club.untitled.domain.Order> existing =
            orderRepository.findByClubIdAndTargetMonth(muje.getId(), thisMonth.toString());
        if (existing.isPresent()) {
            log.info("[DummySeeder] Order 이미 있음 → skip");
            return;
        }

        Long createdBy = orderedish.get(0).getMemberId();
        com.example.demo.club.untitled.domain.Order order = orderRepository.save(
            com.example.demo.club.untitled.domain.Order.create(muje.getId(), thisMonth.toString(), createdBy, 0, 0)
        );

        Map<String, OrderItem> bucket = new HashMap<>();
        int totalAmount = 0;
        int totalQty = 0;
        for (BookRequest br : orderedish) {
            OrderItem item = bucket.get(br.getIsbn());
            if (item == null) {
                item = OrderItem.create(
                    order.getId(),
                    br.getIsbn(), br.getAladinItemCode(),
                    br.getTitle(), br.getAuthor(),
                    br.getPrice(), 1
                );
                orderItemRepository.save(item);
                bucket.put(br.getIsbn(), item);
            } else {
                item.increase(1);
            }
            setOrderIdReflectively(br, order.getId());
            totalAmount += br.getPrice();
            totalQty += 1;
        }
        order.updateTotal(totalAmount, totalQty);
        log.info("[DummySeeder] Order 생성: items={}, totalQty={}, total=₩{}", bucket.size(), totalQty, totalAmount);
    }

    /** Seeder 전용 트릭: BookRequest.assignToOrder는 status도 ORDERED로 강제 변경하므로,
     *  이미 ARRIVED/RECEIVED 상태 책에는 reflection으로 orderId만 세팅. */
    private void setOrderIdReflectively(BookRequest br, Long orderId) {
        try {
            Field f = BookRequest.class.getDeclaredField("orderId");
            f.setAccessible(true);
            f.set(br, orderId);
        } catch (Exception e) {
            log.warn("[DummySeeder] orderId reflective set 실패: {}", e.getMessage());
        }
    }

    private void seedBookReports(Club muje, List<Member> members, List<BookRequest> books) {
        if (members.isEmpty()) return;

        YearMonth thisMonth = YearMonth.now();
        YearMonth lastMonth = thisMonth.minusMonths(1);
        YearMonth twoMonthsAgo = thisMonth.minusMonths(2);

        seedReport(muje, members.get(0), thisMonth, books, "재밌게 읽었어요. 강추!");
        seedReport(muje, members.get(1), thisMonth, books, "생각보다 어려웠지만 끝까지 읽었습니다.");
        seedReport(muje, members.get(2), thisMonth, books, "다음 책도 같은 작가로 읽고 싶어요.");
        seedReport(muje, members.get(3), thisMonth, books, "추천받아 읽었는데 만족스러웠습니다.");

        seedReport(muje, members.get(4), lastMonth, null, "여행 중에 읽기 좋은 책이었어요.");
        seedReport(muje, members.get(5), lastMonth, null, "출퇴근 시간에 짧게 읽기 좋았습니다.");
        seedReport(muje, members.get(6), lastMonth, null, "오랜만에 푹 빠져서 읽었네요.");

        seedReport(muje, members.get(7), twoMonthsAgo, null, "처음 읽어본 장르였는데 신선했어요.");
        seedReport(muje, members.get(0), twoMonthsAgo, null, "주변 사람들에게도 추천했습니다.");
    }

    private void seedReport(Club muje, Member author, YearMonth month, List<BookRequest> bookPool, String coreText) {
        if (bookReportRepository
                .findByClubIdAndMemberIdAndTargetMonth(muje.getId(), author.getId(), month.toString())
                .isPresent()) {
            return;
        }

        BookRequest linked = null;
        if (bookPool != null) {
            linked = bookPool.stream()
                .filter(br -> br.getMemberId().equals(author.getId()))
                .findFirst().orElse(null);
        }

        String bookTitle, bookAuthor, bookThumb;
        Long bookRequestId;
        if (linked != null) {
            bookRequestId = linked.getId();
            bookTitle = linked.getTitle();
            bookAuthor = linked.getAuthor();
            bookThumb = linked.getThumbnailUrl();
        } else {
            bookRequestId = null;
            bookTitle = "외부 도서 - " + author.getName() + " 추천";
            bookAuthor = "미상";
            bookThumb = null;
        }

        String content = (coreText + " ").repeat(5).trim();

        BookReport saved = bookReportRepository.save(BookReport.create(
            bookRequestId, muje.getId(), author.getId(), month.toString(),
            bookTitle, bookAuthor, bookThumb,
            "[" + month + "] " + author.getName() + "의 독후감",
            content
        ));
        if (!month.equals(YearMonth.now())) {
            backdateSubmittedAt(saved, month);
        }
        log.info("[DummySeeder] 독후감: {} ({})", author.getName(), month);
    }

    private void backdateSubmittedAt(BookReport report, YearMonth month) {
        try {
            LocalDateTime backdated = month.atEndOfMonth().atTime(23, 0);
            Field f = BookReport.class.getDeclaredField("submittedAt");
            f.setAccessible(true);
            f.set(report, backdated);
            Field f2 = BookReport.class.getDeclaredField("updatedAt");
            f2.setAccessible(true);
            f2.set(report, backdated);
        } catch (Exception e) {
            log.warn("[DummySeeder] submittedAt 백데이팅 실패: {}", e.getMessage());
        }
    }
}
