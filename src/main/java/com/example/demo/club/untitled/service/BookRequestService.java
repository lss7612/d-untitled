package com.example.demo.club.untitled.service;

import com.example.demo.club.service.ClubService;
import com.example.demo.club.untitled.domain.BookCategory;
import com.example.demo.club.untitled.domain.BookRequest;
import com.example.demo.club.untitled.domain.BookRequestStatus;
import com.example.demo.club.untitled.dto.*;
import com.example.demo.club.untitled.external.AladinApiClient;
import com.example.demo.club.untitled.external.ParsedBook;
import com.example.demo.club.untitled.repository.BookRequestRepository;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookRequestService {

    private final BookRequestRepository bookRequestRepository;
    private final MemberRepository memberRepository;
    private final ClubService clubService;
    private final AladinApiClient aladinApiClient;
    private final ExchangeRateProvider exchangeRateProvider;
    private final BudgetCalculator budgetCalculator;

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

        int limit = budgetCalculator.limitFor(member, targetMonth);
        int used = budgetCalculator.sumUsed(mine);
        // "이번 달 신청 마감 잠금됨" = 비-PENDING 책이 하나라도 있음 (LOCKED 또는 ORDERED 등).
        // 모든 LOCKED → ORDERED로 처리해도 잠금 상태로 인식됨.
        boolean locked = bookRequestRepository.existsByClubIdAndTargetMonthAndStatusIn(
            clubId, targetMonth.toString(),
            java.util.List.of(
                BookRequestStatus.LOCKED, BookRequestStatus.ORDERED,
                BookRequestStatus.SHIPPING, BookRequestStatus.ARRIVED, BookRequestStatus.RECEIVED
            )
        );

        return new MyBookRequestsResponse(
            targetMonth.toString(),
            limit, used, limit - used, locked,
            mine.stream().map(BookRequestResponse::from).toList()
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

        // 잠금 상태 체크 (비-PENDING 책이 하나라도 있으면 마감 진행 중)
        if (bookRequestRepository.existsByClubIdAndTargetMonthAndStatusIn(
                clubId, targetMonth.toString(),
                java.util.List.of(
                    BookRequestStatus.LOCKED, BookRequestStatus.ORDERED,
                    BookRequestStatus.SHIPPING, BookRequestStatus.ARRIVED, BookRequestStatus.RECEIVED
                ))) {
            throw new BusinessException("이번 달 신청이 마감되었습니다.", 400);
        }

        // ISBN 중복
        bookRequestRepository.findByMemberIdAndClubIdAndTargetMonthAndIsbn(
            memberId, clubId, targetMonth.toString(), parsed.isbn()
        ).ifPresent(existing -> {
            throw new BusinessException("이미 이번 달에 신청한 도서입니다.", 400);
        });

        // 예산 초과 체크
        List<BookRequest> mine = bookRequestRepository
            .findAllByMemberIdAndClubIdAndTargetMonthOrderByCreatedAtDesc(memberId, clubId, targetMonth.toString());
        int limit = budgetCalculator.limitFor(member, targetMonth);
        int used = budgetCalculator.sumUsed(mine);
        if (used + priceKrw > limit) {
            throw new BusinessException(
                String.format("잔여 예산 %,d원, 신청 금액 %,d원으로 초과입니다.", limit - used, priceKrw), 400);
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

    @Transactional
    public int lock(Long clubId, Long memberId, YearMonth targetMonth) {
        clubService.requireAdmin(clubId, memberId);
        List<BookRequest> pending = bookRequestRepository
            .findAllByClubIdAndTargetMonthAndStatus(clubId, targetMonth.toString(), BookRequestStatus.PENDING);
        pending.forEach(br -> br.changeStatus(BookRequestStatus.LOCKED));
        return pending.size();
    }

    @Transactional
    public int unlock(Long clubId, Long memberId, YearMonth targetMonth) {
        clubService.requireAdmin(clubId, memberId);
        List<BookRequest> locked = bookRequestRepository
            .findAllByClubIdAndTargetMonthAndStatus(clubId, targetMonth.toString(), BookRequestStatus.LOCKED);
        locked.forEach(br -> br.changeStatus(BookRequestStatus.PENDING));
        return locked.size();
    }
}
