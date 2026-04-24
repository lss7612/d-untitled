package com.example.demo.club.untitled.service;

import com.example.demo.club.service.ClubService;
import com.example.demo.club.untitled.domain.Book;
import com.example.demo.club.untitled.domain.BookExemptionRequest;
import com.example.demo.club.untitled.domain.BookExemptionRequest.Status;
import com.example.demo.club.untitled.dto.BookExemptionResponse;
import com.example.demo.club.untitled.dto.BookResponse;
import com.example.demo.club.untitled.repository.BookExemptionRequestRepository;
import com.example.demo.club.untitled.repository.BookRepository;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 중복된 책 신청에 대한 "제한풀기(exemption)" 플로우 서비스.
 *
 * <ul>
 *   <li>{@link #request} — 회원이 보유 책 중복으로 신청 불가 상태에서 호출. PENDING 으로 기록.</li>
 *   <li>{@link #listPending} — 관리자용 대기 목록.</li>
 *   <li>{@link #approve} — 관리자 승인. {@link Book#grantExemption()} 도 함께 호출하여 이후 중복 체크 통과.</li>
 *   <li>{@link #reject} — 관리자 거절. 책 상태 변화 없음.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookExemptionService {

    private final BookExemptionRequestRepository exemptionRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final ClubService clubService;

    @Transactional
    public BookExemptionResponse request(Long clubId, Long bookId, String reason, Member caller) {
        clubService.requireMembership(clubId, caller.getId());

        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BusinessException("책을 찾을 수 없습니다.", 404));
        if (!book.getClubId().equals(clubId)) {
            throw new BusinessException("다른 동호회의 책에는 신청할 수 없습니다.", 400);
        }
        if (book.isExempt()) {
            throw new BusinessException("이미 제한이 해제된 책입니다. 바로 신청할 수 있습니다.", 400);
        }

        exemptionRepository.findByClubIdAndBookIdAndStatus(clubId, bookId, Status.PENDING)
            .ifPresent(r -> {
                throw new BusinessException("이미 대기 중인 제한풀기 신청이 있습니다.", 409);
            });

        BookExemptionRequest saved = exemptionRepository.save(
            BookExemptionRequest.of(clubId, bookId, caller.getId(), reason));
        log.info("[BookExemption] 신청 clubId={} bookId={} memberId={}",
            clubId, bookId, caller.getId());
        return BookExemptionResponse.of(saved, book, caller);
    }

    public List<BookExemptionResponse> listPending(Long clubId, Member caller) {
        clubService.requireAdmin(clubId, caller.getId(), caller);
        List<BookExemptionRequest> pending = exemptionRepository
            .findAllByClubIdAndStatusOrderByCreatedAtAsc(clubId, Status.PENDING);
        return attachDetails(pending);
    }

    @Transactional
    public BookExemptionResponse approve(Long clubId, Long exemptionId, Member caller) {
        clubService.requireAdmin(clubId, caller.getId(), caller);
        BookExemptionRequest req = loadInClub(clubId, exemptionId);
        if (!req.isPending()) {
            throw new BusinessException("이미 처리된 신청입니다.", 400);
        }
        Book book = bookRepository.findById(req.getBookId())
            .orElseThrow(() -> new BusinessException("대상 책이 삭제되었습니다.", 404));

        book.grantExemption();
        req.approve(caller.getId());
        log.info("[BookExemption] 승인 clubId={} exemptionId={} bookId={} by={}",
            clubId, exemptionId, book.getId(), caller.getId());

        Member requester = memberRepository.findById(req.getMemberId()).orElse(null);
        return BookExemptionResponse.of(req, book, requester);
    }

    @Transactional
    public BookExemptionResponse reject(Long clubId, Long exemptionId, Member caller) {
        clubService.requireAdmin(clubId, caller.getId(), caller);
        BookExemptionRequest req = loadInClub(clubId, exemptionId);
        if (!req.isPending()) {
            throw new BusinessException("이미 처리된 신청입니다.", 400);
        }
        req.reject(caller.getId());
        log.info("[BookExemption] 거절 clubId={} exemptionId={} by={}",
            clubId, exemptionId, caller.getId());

        Book book = bookRepository.findById(req.getBookId()).orElse(null);
        Member requester = memberRepository.findById(req.getMemberId()).orElse(null);
        return BookExemptionResponse.of(req, book, requester);
    }

    /** 관리자: 제한풀기가 승인되어 현재 중복 체크 우회 중인 책 목록. */
    public List<BookResponse> listExempt(Long clubId, Member caller) {
        clubService.requireAdmin(clubId, caller.getId(), caller);
        return bookRepository
            .findAllByClubIdAndExemptionGrantedAtIsNotNullOrderByExemptionGrantedAtDesc(clubId)
            .stream()
            .map(BookResponse::from)
            .toList();
    }

    /**
     * 관리자: 책의 exemption 을 해제하여 다시 중복 체크 대상으로 되돌린다.
     *
     * Idempotent — 이미 비-exempt 상태여도 성공으로 처리한다 (경쟁 상태에서 UX 마찰 회피).
     * 기존 {@link BookExemptionRequest} 이력(APPROVED)은 보존하며, 회원이 새 PENDING 을 다시 넣을 수 있다.
     */
    @Transactional
    public BookResponse revoke(Long clubId, Long bookId, Member caller) {
        clubService.requireAdmin(clubId, caller.getId(), caller);
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BusinessException("책을 찾을 수 없습니다.", 404));
        if (!book.getClubId().equals(clubId)) {
            throw new BusinessException("다른 동호회의 책에는 접근할 수 없습니다.", 403);
        }
        if (book.isExempt()) {
            book.revokeExemption();
            log.info("[BookExemption] 재적용 clubId={} bookId={} by={}",
                clubId, bookId, caller.getId());
        }
        return BookResponse.from(book);
    }

    private BookExemptionRequest loadInClub(Long clubId, Long exemptionId) {
        BookExemptionRequest req = exemptionRepository.findById(exemptionId)
            .orElseThrow(() -> new BusinessException("신청을 찾을 수 없습니다.", 404));
        if (!req.getClubId().equals(clubId)) {
            throw new BusinessException("다른 동호회의 신청에는 접근할 수 없습니다.", 403);
        }
        return req;
    }

    private List<BookExemptionResponse> attachDetails(List<BookExemptionRequest> reqs) {
        if (reqs.isEmpty()) return List.of();
        List<Long> bookIds = reqs.stream().map(BookExemptionRequest::getBookId).distinct().toList();
        List<Long> memberIds = reqs.stream().map(BookExemptionRequest::getMemberId).distinct().toList();
        Map<Long, Book> booksById = new HashMap<>();
        bookRepository.findAllById(bookIds).forEach(b -> booksById.put(b.getId(), b));
        Map<Long, Member> membersById = new HashMap<>();
        memberRepository.findAllById(memberIds).forEach(m -> membersById.put(m.getId(), m));
        return reqs.stream()
            .map(r -> BookExemptionResponse.of(r, booksById.get(r.getBookId()), membersById.get(r.getMemberId())))
            .toList();
    }
}
