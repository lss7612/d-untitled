package com.example.demo.club.untitled.budget.service;

import com.example.demo.club.domain.ClubMember;
import com.example.demo.club.domain.ClubMember.MembershipStatus;
import com.example.demo.club.repository.ClubMemberRepository;
import com.example.demo.club.service.ClubService;
import com.example.demo.club.untitled.budget.domain.BudgetShare;
import com.example.demo.club.untitled.budget.domain.BudgetShareStatus;
import com.example.demo.club.untitled.budget.dto.BudgetShareResponse;
import com.example.demo.club.untitled.budget.dto.ShareCandidateResponse;
import com.example.demo.club.untitled.budget.repository.BudgetShareRepository;
import com.example.demo.club.untitled.domain.BookRequestStatus;
import com.example.demo.club.untitled.repository.BookRequestRepository;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.notification.NotificationService;
import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 예산 나눔(BudgetShare) 오케스트레이션.
 * - PENDING 생성 / ACCEPTED 전이 시점 모두 멤버별 월별 lock 을 재검사 (나눔 참여자가 한 명이라도 LOCK 상태면 409).
 * - 수락 시점 sender 의 실시간 remaining 재검증 → 동시성으로 초과되지 않게 가드.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetShareService {

    /** 나눔 이체가 동결되는 BookRequest 상태들 (멤버 단위로 하나라도 있으면 lock). */
    private static final List<BookRequestStatus> LOCKED_STATUSES = List.of(
        BookRequestStatus.LOCKED,
        BookRequestStatus.ORDERED,
        BookRequestStatus.SHIPPING,
        BookRequestStatus.ARRIVED,
        BookRequestStatus.RECEIVED
    );

    private final BudgetShareRepository budgetShareRepository;
    private final ClubService clubService;
    private final ClubMemberRepository clubMemberRepository;
    private final MemberRepository memberRepository;
    private final BookRequestRepository bookRequestRepository;
    private final MemberBudgetService memberBudgetService;
    private final NotificationService notificationService;

    /** 본인 제외 ACTIVE 멤버 리스트 + 각자의 이체 반영 remaining. */
    public List<ShareCandidateResponse> listCandidates(Long clubId, YearMonth ym, Member caller) {
        clubService.requireMembership(clubId, caller.getId());
        List<ClubMember> actives = clubMemberRepository.findAllByClubIdAndStatus(clubId, MembershipStatus.ACTIVE);
        List<Long> memberIds = actives.stream()
            .map(ClubMember::getMemberId)
            .filter(id -> !id.equals(caller.getId()))
            .toList();
        if (memberIds.isEmpty()) return List.of();

        Map<Long, Member> memberById = memberRepository.findAllById(memberIds).stream()
            .collect(Collectors.toMap(Member::getId, m -> m));

        return memberIds.stream()
            .map(memberById::get)
            .filter(java.util.Objects::nonNull)
            .map(m -> new ShareCandidateResponse(
                m.getId(),
                m.getName(),
                m.getEmail(),
                memberBudgetService.getRemaining(clubId, m, ym)
            ))
            .toList();
    }

    /** 내게 들어온 PENDING (sender=me). */
    public List<BudgetShareResponse> listIncomingPending(Long clubId, YearMonth ym, Member caller) {
        clubService.requireMembership(clubId, caller.getId());
        return toResponses(budgetShareRepository
            .findAllByClubIdAndTargetMonthAndSenderIdAndStatus(
                clubId, ym.toString(), caller.getId(), BudgetShareStatus.PENDING));
    }

    /** 내가 보낸 PENDING (requester=me). */
    public List<BudgetShareResponse> listOutgoingPending(Long clubId, YearMonth ym, Member caller) {
        clubService.requireMembership(clubId, caller.getId());
        return toResponses(budgetShareRepository
            .findAllByClubIdAndTargetMonthAndRequesterIdAndStatus(
                clubId, ym.toString(), caller.getId(), BudgetShareStatus.PENDING));
    }

    /** 내가 requester 또는 sender 인 ACCEPTED (배너용). */
    public List<BudgetShareResponse> listAcceptedInvolving(Long clubId, YearMonth ym, Member caller) {
        clubService.requireMembership(clubId, caller.getId());
        return toResponses(budgetShareRepository
            .findAcceptedInvolving(clubId, ym.toString(), caller.getId()));
    }

    @Transactional
    public BudgetShareResponse createRequest(
        Long clubId, YearMonth ym, Member requester,
        Long senderId, int amount, String note
    ) {
        clubService.requireMembership(clubId, requester.getId());
        if (senderId == null || senderId.equals(requester.getId())) {
            throw new BusinessException("자기 자신에게 나눔 신청할 수 없습니다.", 400);
        }
        if (amount <= 0) {
            throw new BusinessException("금액은 양의 정수여야 합니다.", 400);
        }

        ClubMember senderMembership = clubMemberRepository.findByClubIdAndMemberId(clubId, senderId)
            .orElseThrow(() -> new BusinessException("전달할 회원을 찾을 수 없습니다.", 404));
        if (!senderMembership.isActive()) {
            throw new BusinessException("전달할 회원이 클럽 멤버가 아닙니다.", 400);
        }

        Member sender = memberRepository.findById(senderId)
            .orElseThrow(() -> new BusinessException("전달할 회원을 찾을 수 없습니다.", 404));

        // 월 전환 보완: 양측 모두 해당 월 스냅샷이 존재하도록 보장.
        memberBudgetService.getOrCreate(clubId, requester, ym);
        memberBudgetService.getOrCreate(clubId, sender, ym);

        // 락 가드: 양측 모두 LOCK 상태 아닌지 확인
        requireNotLocked(clubId, requester.getId(), ym, "본인의");
        requireNotLocked(clubId, senderId, ym, "전달할 회원의");

        // 중복 PENDING 금지
        budgetShareRepository.findByClubIdAndTargetMonthAndRequesterIdAndSenderIdAndStatus(
            clubId, ym.toString(), requester.getId(), senderId, BudgetShareStatus.PENDING
        ).ifPresent(existing -> {
            throw new BusinessException("이미 해당 회원에게 보낸 대기 중인 나눔 신청이 있습니다.", 409);
        });

        BudgetShare saved = budgetShareRepository.save(
            BudgetShare.create(clubId, ym, requester.getId(), senderId, amount, note)
        );
        notificationService.onBudgetShareRequested(saved, requester, sender);
        return BudgetShareResponse.of(saved, requester, sender);
    }

    @Transactional
    public BudgetShareResponse accept(Long clubId, Long shareId, Member caller) {
        clubService.requireMembership(clubId, caller.getId());
        BudgetShare share = loadInClub(clubId, shareId);
        if (!share.getSenderId().equals(caller.getId())) {
            throw new BusinessException("이 나눔 신청을 수락할 수 없습니다.", 403);
        }
        if (!share.isPending()) {
            throw new BusinessException("이미 처리된 나눔 신청입니다.", 409);
        }

        YearMonth ym = share.getTargetYearMonth();
        requireNotLocked(clubId, share.getRequesterId(), ym, "요청 회원의");
        requireNotLocked(clubId, share.getSenderId(), ym, "본인의");

        // 월 전환 보완: 수락 시점 기준으로도 양측 스냅샷 보장.
        Member requesterForSnapshot = memberRepository.findById(share.getRequesterId())
            .orElseThrow(() -> new BusinessException("요청 회원을 찾을 수 없습니다.", 404));
        memberBudgetService.getOrCreate(clubId, requesterForSnapshot, ym);
        memberBudgetService.getOrCreate(clubId, caller, ym);

        // 실시간 remaining 재검증 (동시성 가드)
        int senderRemaining = memberBudgetService.getRemaining(clubId, caller, ym);
        if (senderRemaining < share.getAmount()) {
            throw new BusinessException(
                String.format("잔여 예산 %,d원으로 나눔 금액 %,d원을 수락할 수 없습니다.",
                    senderRemaining, share.getAmount()),
                409
            );
        }

        share.accept();

        Member requester = memberRepository.findById(share.getRequesterId()).orElse(null);
        notificationService.onBudgetShareAccepted(share, requester, caller);
        return BudgetShareResponse.of(share, requester, caller);
    }

    @Transactional
    public BudgetShareResponse reject(Long clubId, Long shareId, Member caller) {
        clubService.requireMembership(clubId, caller.getId());
        BudgetShare share = loadInClub(clubId, shareId);
        if (!share.getSenderId().equals(caller.getId())) {
            throw new BusinessException("이 나눔 신청을 거절할 수 없습니다.", 403);
        }
        if (!share.isPending()) {
            throw new BusinessException("이미 처리된 나눔 신청입니다.", 409);
        }
        share.reject();
        return toResponse(share);
    }

    @Transactional
    public BudgetShareResponse cancel(Long clubId, Long shareId, Member caller) {
        clubService.requireMembership(clubId, caller.getId());
        BudgetShare share = loadInClub(clubId, shareId);
        if (!share.getRequesterId().equals(caller.getId())) {
            throw new BusinessException("본인이 보낸 나눔 신청만 취소할 수 있습니다.", 403);
        }
        if (!share.isPending()) {
            throw new BusinessException("이미 처리된 나눔 신청입니다.", 409);
        }
        share.cancel();
        return toResponse(share);
    }

    // --- helpers ---

    private BudgetShare loadInClub(Long clubId, Long shareId) {
        BudgetShare share = budgetShareRepository.findById(shareId)
            .orElseThrow(() -> new BusinessException("나눔 신청을 찾을 수 없습니다.", 404));
        if (!share.getClubId().equals(clubId)) {
            throw new BusinessException("다른 동호회의 나눔 신청입니다.", 400);
        }
        return share;
    }

    private void requireNotLocked(Long clubId, Long memberId, YearMonth ym, String subject) {
        boolean locked = bookRequestRepository.existsByClubIdAndMemberIdAndTargetMonthAndStatusIn(
            clubId, memberId, ym.toString(), LOCKED_STATUSES
        );
        if (locked) {
            throw new BusinessException(subject + " 이번 달 신청이 이미 마감되어 나눔을 변경할 수 없습니다.", 409);
        }
    }

    private BudgetShareResponse toResponse(BudgetShare share) {
        Member requester = memberRepository.findById(share.getRequesterId()).orElse(null);
        Member sender = memberRepository.findById(share.getSenderId()).orElse(null);
        return BudgetShareResponse.of(share, requester, sender);
    }

    private List<BudgetShareResponse> toResponses(List<BudgetShare> shares) {
        if (shares.isEmpty()) return List.of();
        java.util.Set<Long> memberIds = new java.util.HashSet<>();
        for (BudgetShare s : shares) {
            memberIds.add(s.getRequesterId());
            memberIds.add(s.getSenderId());
        }
        Map<Long, Member> memberById = memberRepository.findAllById(memberIds).stream()
            .collect(Collectors.toMap(Member::getId, m -> m));
        return shares.stream()
            .map(s -> BudgetShareResponse.of(s, memberById.get(s.getRequesterId()), memberById.get(s.getSenderId())))
            .toList();
    }
}
