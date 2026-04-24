package com.example.demo.club.untitled.service;

import com.example.demo.club.domain.ClubMember;
import com.example.demo.club.domain.ClubMember.MembershipStatus;
import com.example.demo.club.repository.ClubMemberRepository;
import com.example.demo.club.service.ClubService;
import com.example.demo.club.untitled.budget.dto.BudgetShareResponse;
import com.example.demo.club.untitled.budget.service.BudgetShareService;
import com.example.demo.club.untitled.budget.service.MemberBudgetService;
import com.example.demo.club.untitled.domain.Book;
import com.example.demo.club.untitled.domain.BookCategory;
import com.example.demo.club.untitled.domain.BookRequest;
import com.example.demo.club.untitled.domain.BookRequestStatus;
import com.example.demo.club.untitled.dto.*;
import com.example.demo.club.untitled.external.AladinApiClient;
import com.example.demo.club.untitled.external.ParsedBook;
import com.example.demo.club.untitled.repository.BookRepository;
import com.example.demo.club.untitled.repository.BookRequestRepository;
import com.example.demo.club.untitled.util.BookNames;
import lombok.extern.slf4j.Slf4j;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookRequestService {

    private final BookRequestRepository bookRequestRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final ClubService clubService;
    private final AladinApiClient aladinApiClient;
    private final ExchangeRateProvider exchangeRateProvider;
    private final MemberBudgetService memberBudgetService;
    private final BudgetShareService budgetShareService;
    private final ClubMemberRepository clubMemberRepository;
    private final MonthLockService monthLockService;

    public ParsedBookResponse parseUrl(Long clubId, Long memberId, ParseUrlRequest req) {
        clubService.requireMembership(clubId, memberId);
        ParsedBook book = aladinApiClient.parse(req.url());
        BigDecimal rate = exchangeRateProvider.rateFor(book.currency());
        int priceKrw = book.price().multiply(rate).setScale(0, RoundingMode.HALF_UP).intValue();
        return ParsedBookResponse.of(book, rate, priceKrw);
    }

    public List<BookRequestResponse> findAllByMonth(Long clubId, Long memberId, YearMonth targetMonth) {
        clubService.requireMembership(clubId, memberId);
        return bookRequestRepository.findAllByClubIdAndTargetMonthOrderByCreatedAtDesc(clubId, targetMonth.toString())
            .stream().map(BookRequestResponse::from).toList();
    }

    public MyBookRequestsResponse findMine(Long clubId, Long memberId, YearMonth targetMonth) {
        clubService.requireMembership(clubId, memberId);
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException("회원 정보를 찾을 수 없습니다.", 404));

        List<BookRequest> mine = bookRequestRepository
            .findAllByMemberIdAndClubIdAndTargetMonthOrderByCreatedAtDesc(memberId, clubId, targetMonth.toString());

        int baseLimit = memberBudgetService.getBaseLimit(clubId, member, targetMonth);
        int transferIn = memberBudgetService.getTransferIn(clubId, memberId, targetMonth);
        int transferOut = memberBudgetService.getTransferOut(clubId, memberId, targetMonth);
        int effectiveLimit = baseLimit + transferIn - transferOut;
        int used = mine.stream().mapToInt(BookRequest::getPrice).sum();
        int remaining = effectiveLimit - used;

        // 월별 잠금 플래그는 month_locks 테이블에 독립적으로 저장됨.
        boolean locked = monthLockService.isLocked(clubId, targetMonth);

        List<BudgetShareResponse> acceptedShares = budgetShareService
            .listAcceptedInvolving(clubId, targetMonth, member);

        return new MyBookRequestsResponse(
            targetMonth.toString(),
            baseLimit, transferIn, transferOut, effectiveLimit,
            used, remaining, locked,
            mine.stream().map(BookRequestResponse::from).toList(),
            acceptedShares
        );
    }

    @Transactional
    public BookRequestResponse create(Long clubId, Long memberId, BookRequestCreateRequest req) {
        clubService.requireMembership(clubId, memberId);
        if (req.category() == null) {
            throw new BusinessException("카테고리를 선택해주세요.", 400);
        }
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException("회원 정보를 찾을 수 없습니다.", 404));

        ParsedBook parsed = aladinApiClient.parse(req.url());
        BigDecimal rate = exchangeRateProvider.rateFor(parsed.currency());
        int priceKrw = parsed.price().multiply(rate).setScale(0, RoundingMode.HALF_UP).intValue();

        YearMonth targetMonth = YearMonth.now();

        // 월 전환 보완: 쓰기 시점에 이번 달 스냅샷이 없으면 정책값으로 INSERT.
        // (가입 승인 시점에 가입 당시 월은 이미 박혀 있지만, 월이 바뀐 뒤 처음 쓰기 액션에서 필요.)
        memberBudgetService.getOrCreate(clubId, member, targetMonth);

        // 잠금 상태 체크 — month_locks 테이블의 플래그.
        if (monthLockService.isLocked(clubId, targetMonth)) {
            throw new BusinessException("이번 달 신청이 마감되었습니다.", 400);
        }

        // ISBN 중복 (같은 멤버, 같은 달 내에서)
        bookRequestRepository.findByMemberIdAndClubIdAndTargetMonthAndIsbn(
            memberId, clubId, targetMonth.toString(), parsed.isbn()
        ).ifPresent(existing -> {
            throw new BusinessException("이미 이번 달에 신청한 도서입니다.", 400);
        });

        // 정규화 키는 한 번만 계산 (카탈로그 + cross-member 체크에서 재사용).
        String normTitle = BookNames.normalize(parsed.title());
        String normAuthor = BookNames.normalize(parsed.author());

        // 클럽 전체 카탈로그(`books`) 기준 중복 — (title, author) 정규화 매칭.
        // 제한풀기(exemption)가 승인된 책은 예외적으로 통과.
        java.util.Optional<Book> catalogHit = bookRepository
            .findByClubIdAndNormalizedTitleAndNormalizedAuthor(clubId, normTitle, normAuthor);
        catalogHit.filter(b -> !b.isExempt()).ifPresent(b -> {
            throw new BusinessException(
                "이미 보유 중인 책입니다. 필요 시 제한풀기 신청을 해주세요.",
                409,
                Map.of(
                    "code", "DUPLICATE_BOOK",
                    "duplicateBookId", b.getId(),
                    "duplicateBookTitle", b.getTitle()
                )
            );
        });

        // 타 멤버가 이번 달에 같은 책을 이미 신청 중인지 (PENDING 포함 전 상태).
        // exempt 승인된 책은 "여러 권 허용" 이 취지이므로 이 체크에서 제외.
        if (catalogHit.isEmpty()) {
            BookRequest conflict = bookRequestRepository
                .findAllByClubIdAndTargetMonth(clubId, targetMonth.toString())
                .stream()
                .filter(br -> !br.getMemberId().equals(memberId))
                .filter(br ->
                    (parsed.isbn() != null && parsed.isbn().equals(br.getIsbn()))
                    || (normTitle.equals(BookNames.normalize(br.getTitle()))
                        && normAuthor.equals(BookNames.normalize(br.getAuthor())))
                )
                .findFirst()
                .orElse(null);

            if (conflict != null) {
                Member requester = memberRepository.findById(conflict.getMemberId()).orElse(null);
                String name = requester == null ? "다른 회원" : requester.getName();
                String email = requester == null ? null : requester.getEmail();
                // 동명이인 구분을 위해 이메일 병기: "홍길동(user@example.com)님이 …"
                String display = email == null ? name : name + "(" + email + ")";
                Map<String, Object> details = new java.util.LinkedHashMap<>();
                details.put("code", "DUPLICATE_MONTHLY_REQUEST");
                details.put("requesterMemberId", conflict.getMemberId());
                details.put("requesterName", name);
                if (email != null) details.put("requesterEmail", email);
                details.put("bookTitle", conflict.getTitle());
                throw new BusinessException(
                    display + "님이 이번 달에 이미 신청하고 있는 책입니다.",
                    409,
                    details
                );
            }
        }

        // 예산 초과 체크 (Phase 2에서 나눔 이체 반영 시 remaining 내부 로직이 확장됨)
        int remaining = memberBudgetService.getRemaining(clubId, member, targetMonth);
        if (priceKrw > remaining) {
            throw new BusinessException(
                String.format("잔여 예산 %,d원, 신청 금액 %,d원으로 초과입니다.", remaining, priceKrw), 400);
        }

        BookRequest entity = BookRequest.create(
            clubId, memberId,
            parsed.title(), parsed.author(), parsed.publisher(), parsed.isbn(),
            priceKrw, parsed.price(), parsed.currency(), rate,
            req.category(), parsed.sourceUrl(), parsed.thumbnailUrl(), parsed.aladinItemCode(),
            targetMonth
        );
        return BookRequestResponse.from(bookRequestRepository.save(entity));
    }

    @Transactional
    public BookRequestResponse update(Long clubId, Long memberId, Long id, BookRequestUpdateRequest req) {
        clubService.requireMembership(clubId, memberId);
        BookRequest br = loadOwned(id, clubId, memberId);
        if (!br.isEditable()) {
            throw new BusinessException("수정할 수 없는 상태입니다.", 400);
        }
        if (req.category() != null) {
            br.edit(req.category());
        }
        return BookRequestResponse.from(br);
    }

    @Transactional
    public void delete(Long clubId, Long memberId, Long id) {
        clubService.requireMembership(clubId, memberId);
        BookRequest br = loadOwned(id, clubId, memberId);
        if (!br.isEditable()) {
            throw new BusinessException("취소할 수 없는 상태입니다.", 400);
        }
        bookRequestRepository.delete(br);
    }

    private BookRequest loadOwned(Long id, Long clubId, Long memberId) {
        BookRequest br = bookRequestRepository.findById(id)
            .orElseThrow(() -> new BusinessException("신청을 찾을 수 없습니다.", 404));
        if (!br.getClubId().equals(clubId) || !br.getMemberId().equals(memberId)) {
            throw new BusinessException("본인의 신청만 수정/취소할 수 있습니다.", 403);
        }
        return br;
    }

    public List<BookCategory> categories() {
        return List.of(BookCategory.values());
    }

    /**
     * 관리자: 월별 신청 접수 잠금. {@link MonthLock} 플래그만 토글하고 BookRequest 상태는 건드리지 않는다.
     * 반환: 토글 후 잠금 상태.
     */
    @Transactional
    public boolean lock(Long clubId, Long adminMemberId, YearMonth targetMonth, Member caller) {
        return monthLockService.lock(clubId, adminMemberId, targetMonth, caller);
    }

    @Transactional
    public boolean unlock(Long clubId, Long adminMemberId, YearMonth targetMonth, Member caller) {
        return monthLockService.unlock(clubId, adminMemberId, targetMonth, caller);
    }

    /** 관리자: 선택한 신청들을 ORDERED → ARRIVED. */
    @Transactional
    public int markArrived(Long clubId, Long adminMemberId, List<Long> ids) {
        clubService.requireAdmin(clubId, adminMemberId);
        return mutateStatuses(clubId, ids, BookRequestStatus.ORDERED, BookRequest::markArrived);
    }

    /** 관리자: 선택한 신청들을 ARRIVED → ORDERED (도착 처리 취소). */
    @Transactional
    public int markUnarrived(Long clubId, Long adminMemberId, List<Long> ids) {
        clubService.requireAdmin(clubId, adminMemberId);
        return mutateStatuses(clubId, ids, BookRequestStatus.ARRIVED, BookRequest::markUnarrived);
    }

    /** 회원 본인: 선택한 책들을 ARRIVED → RECEIVED. */
    @Transactional
    public int markReceived(Long clubId, Long memberId, List<Long> ids) {
        clubService.requireMembership(clubId, memberId);
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("선택한 책이 없습니다.", 400);
        }
        List<BookRequest> targets = bookRequestRepository.findAllById(ids);
        if (targets.size() != ids.size()) {
            throw new BusinessException("일부 신청을 찾을 수 없습니다.", 404);
        }
        for (BookRequest br : targets) {
            if (!br.getClubId().equals(clubId)) {
                throw new BusinessException("다른 동호회의 신청이 포함되었습니다.", 400);
            }
            if (!br.getMemberId().equals(memberId)) {
                throw new BusinessException("본인이 신청한 책만 수령 처리할 수 있습니다.", 403);
            }
            if (br.getStatus() != BookRequestStatus.ARRIVED) {
                throw new BusinessException("ARRIVED 상태가 아닌 신청이 포함되었습니다: " + br.getStatus().getLabel(), 400);
            }
        }
        for (BookRequest br : targets) {
            br.markReceived();
            upsertCatalogEntry(br);
        }
        return targets.size();
    }

    /**
     * BookRequest 가 RECEIVED 로 전이될 때 {@code books} 카탈로그를 동기화한다.
     * - 같은 (clubId, normalizedTitle, normalizedAuthor) 책이 있으면 copies 를 +1.
     * - 없으면 새 Book 을 insert (copies=1). 기존에 수동 시드에만 의존하던 한계 해소.
     */
    private void upsertCatalogEntry(BookRequest br) {
        String normTitle = BookNames.normalize(br.getTitle());
        String normAuthor = BookNames.normalize(br.getAuthor());
        bookRepository.findByClubIdAndNormalizedTitleAndNormalizedAuthor(
                br.getClubId(), normTitle, normAuthor)
            .ifPresentOrElse(
                Book::incrementCopies,
                () -> {
                    BigDecimal priceBd = br.getPrice() == null ? null : BigDecimal.valueOf(br.getPrice());
                    bookRepository.save(Book.of(
                        br.getClubId(),
                        br.getTitle(),
                        br.getAuthor(),
                        br.getCategory(),
                        priceBd,
                        br.getSourceUrl(),
                        br.getThumbnailUrl()
                    ));
                    log.info("[BookCatalog] 신규 편입 clubId={} title={} author={}",
                        br.getClubId(), br.getTitle(), br.getAuthor());
                }
            );
    }

    /**
     * 관리자 전용: 특정 월에 책 신청을 하지 않은 ACTIVE 멤버 목록.
     * - "제출했음" 기준: 해당 클럽·월에 {@link BookRequest} 가 1건이라도 있으면 제출로 간주 (상태 무관).
     * - DEVELOPER 는 requireAdmin bypass.
     */
    public UnsubmittedMembersResponse findUnsubmittedForAdmin(Long clubId, Long callerId, YearMonth ym, Member caller) {
        clubService.requireAdmin(clubId, callerId, caller);

        List<ClubMember> actives = clubMemberRepository.findAllByClubIdAndStatus(clubId, MembershipStatus.ACTIVE);
        List<Long> activeIds = actives.stream().map(ClubMember::getMemberId).toList();

        List<BookRequest> requests = bookRequestRepository
            .findAllByClubIdAndTargetMonthOrderByCreatedAtDesc(clubId, ym.toString());
        Set<Long> submittedIds = new HashSet<>();
        for (BookRequest br : requests) submittedIds.add(br.getMemberId());

        Map<Long, Member> memberById = memberRepository.findAllById(activeIds).stream()
            .collect(Collectors.toMap(Member::getId, m -> m));

        List<UnsubmittedMembersResponse.UnsubmittedMember> unsubmitted = activeIds.stream()
            .filter(id -> !submittedIds.contains(id))
            .map(memberById::get)
            .filter(java.util.Objects::nonNull)
            .map(m -> new UnsubmittedMembersResponse.UnsubmittedMember(m.getId(), m.getName(), m.getEmail()))
            .toList();

        int totalActive = activeIds.size();
        int submittedCount = (int) activeIds.stream().filter(submittedIds::contains).count();
        return new UnsubmittedMembersResponse(ym.toString(), totalActive, submittedCount, unsubmitted);
    }

    private int mutateStatuses(Long clubId, List<Long> ids, BookRequestStatus expected, java.util.function.Consumer<BookRequest> mutator) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("선택한 책이 없습니다.", 400);
        }
        List<BookRequest> targets = bookRequestRepository.findAllById(ids);
        if (targets.size() != ids.size()) {
            throw new BusinessException("일부 신청을 찾을 수 없습니다.", 404);
        }
        for (BookRequest br : targets) {
            if (!br.getClubId().equals(clubId)) {
                throw new BusinessException("다른 동호회의 신청이 포함되었습니다.", 400);
            }
            if (br.getStatus() != expected) {
                throw new BusinessException(expected.getLabel() + " 상태가 아닌 신청이 포함되었습니다: " + br.getStatus().getLabel(), 400);
            }
        }
        targets.forEach(mutator);
        return targets.size();
    }
}
