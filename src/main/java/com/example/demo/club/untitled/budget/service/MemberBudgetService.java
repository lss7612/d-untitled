package com.example.demo.club.untitled.budget.service;

import com.example.demo.club.domain.ClubMember;
import com.example.demo.club.domain.ClubMember.MembershipStatus;
import com.example.demo.club.repository.ClubMemberRepository;
import com.example.demo.club.service.ClubService;
import com.example.demo.club.untitled.budget.domain.MemberMonthlyBudget;
import com.example.demo.club.untitled.budget.dto.BudgetSummaryResponse;
import com.example.demo.club.untitled.budget.repository.BudgetShareRepository;
import com.example.demo.club.untitled.budget.repository.MemberMonthlyBudgetRepository;
import com.example.demo.club.untitled.domain.BookRequest;
import com.example.demo.club.untitled.repository.BookRequestRepository;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

/**
 * 멤버 월별 예산 조회·조정 오케스트레이션.
 * - 스냅샷 lazy 생성 ({@link #getOrCreate})
 * - 현재 사용량은 BookRequest 가격 합계로 즉시 집계
 * - Phase 2 에서 transferIn/Out 를 이 서비스가 흡수하여 effectiveLimit 로 확장될 예정
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberBudgetService {

    private final MemberMonthlyBudgetRepository budgetRepository;
    private final BudgetShareRepository budgetShareRepository;
    private final BookRequestRepository bookRequestRepository;
    private final MemberRepository memberRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubService clubService;
    private final BudgetPolicy budgetPolicy;

    /**
     * 해당 (club, member, yearMonth) 의 스냅샷을 반환.
     * 없으면 정책으로 계산된 값으로 생성해 저장.
     *
     * <p><b>주의: 이 메서드는 DB INSERT 를 수행한다.</b> 반드시 쓰기 트랜잭션에서 호출할 것.
     * 읽기 경로에서는 {@link #getBaseLimit(Long, Member, YearMonth)} 처럼 save 하지 않는 메서드를 사용.
     * 스냅샷의 정규 생성 지점은 <b>클럽 가입 승인 시점</b> (ClubMembershipService#approve/ensureDeveloperClubMemberships)
     * 이며, 월 전환 보완을 위해 책 신청 생성/나눔 신청·수락 같은 쓰기 경로도 이 메서드를 호출해 스냅샷을 보장한다.
     */
    @Transactional
    public MemberMonthlyBudget getOrCreate(Long clubId, Member member, YearMonth yearMonth) {
        return budgetRepository
            .findByClubIdAndMemberIdAndYearMonth(clubId, member.getId(), yearMonth.toString())
            .orElseGet(() -> {
                int limit = budgetPolicy.computeBaseLimit(member, yearMonth);
                return budgetRepository.save(
                    MemberMonthlyBudget.of(clubId, member.getId(), yearMonth, limit)
                );
            });
    }

    /**
     * 해당 월의 baseLimit 을 반환.
     * <p>스냅샷이 없으면 정책으로 계산해 반환 (save 하지 않음). 읽기 트랜잭션 안전.
     */
    public int getBaseLimit(Long clubId, Member member, YearMonth yearMonth) {
        return budgetRepository
            .findByClubIdAndMemberIdAndYearMonth(clubId, member.getId(), yearMonth.toString())
            .map(MemberMonthlyBudget::getBaseLimit)
            .orElseGet(() -> budgetPolicy.computeBaseLimit(member, yearMonth));
    }

    public int getUsed(Long clubId, Long memberId, YearMonth yearMonth) {
        List<BookRequest> mine = bookRequestRepository
            .findAllByMemberIdAndClubIdAndTargetMonthOrderByCreatedAtDesc(memberId, clubId, yearMonth.toString());
        return mine.stream().mapToInt(BookRequest::getPrice).sum();
    }

    /** 내가 requester 인 ACCEPTED 이체의 합계 (내가 받은 금액). */
    public int getTransferIn(Long clubId, Long memberId, YearMonth yearMonth) {
        return budgetShareRepository.sumAcceptedIn(clubId, yearMonth.toString(), memberId);
    }

    /** 내가 sender 인 ACCEPTED 이체의 합계 (내가 준 금액). */
    public int getTransferOut(Long clubId, Long memberId, YearMonth yearMonth) {
        return budgetShareRepository.sumAcceptedOut(clubId, yearMonth.toString(), memberId);
    }

    /** 이체 반영된 실한도 = base + in − out. */
    public int getEffectiveLimit(Long clubId, Member member, YearMonth yearMonth) {
        int base = getBaseLimit(clubId, member, yearMonth);
        int in = getTransferIn(clubId, member.getId(), yearMonth);
        int out = getTransferOut(clubId, member.getId(), yearMonth);
        return base + in - out;
    }

    /** remaining = effectiveLimit - used. 나눔 이체 반영. */
    public int getRemaining(Long clubId, Member member, YearMonth yearMonth) {
        return getEffectiveLimit(clubId, member, yearMonth) - getUsed(clubId, member.getId(), yearMonth);
    }

    /** 관리자가 특정 멤버의 한도를 조정. DEVELOPER 는 ADMIN 체크 bypass. */
    @Transactional
    public MemberMonthlyBudget adjust(Long clubId, Long memberId, YearMonth yearMonth,
                                      int newLimit, String reason, Member caller) {
        clubService.requireAdmin(clubId, caller.getId(), caller);

        Member target = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException("대상 회원을 찾을 수 없습니다.", 404));

        MemberMonthlyBudget budget = getOrCreate(clubId, target, yearMonth);
        budget.adjust(newLimit, reason);
        return budget;
    }

    /**
     * 관리자 요약: 월별 클럽 전체 예산 사용 현황.
     * - totalRequestedAmount: 상태 무관 BookRequest.price 합계
     * - totalBaseLimit: ACTIVE 멤버의 baseLimit 합계 (나눔은 클럽 내부 이전이라 총합 불변이므로 base 합으로 충분)
     * - usagePercent: totalBaseLimit 이 0 이면 0.0
     * DEVELOPER 는 requireAdmin bypass.
     */
    public BudgetSummaryResponse summaryForAdmin(Long clubId, Long callerId, YearMonth yearMonth, Member caller) {
        clubService.requireAdmin(clubId, callerId, caller);

        List<ClubMember> actives = clubMemberRepository.findAllByClubIdAndStatus(clubId, MembershipStatus.ACTIVE);
        List<Long> activeIds = actives.stream().map(ClubMember::getMemberId).toList();
        List<Member> activeMembers = memberRepository.findAllById(activeIds);

        int totalRequestedAmount = bookRequestRepository
            .findAllByClubIdAndTargetMonthOrderByCreatedAtDesc(clubId, yearMonth.toString())
            .stream().mapToInt(BookRequest::getPrice).sum();

        int totalBaseLimit = activeMembers.stream()
            .mapToInt(m -> getBaseLimit(clubId, m, yearMonth))
            .sum();

        double usagePercent = totalBaseLimit > 0
            ? (double) totalRequestedAmount / totalBaseLimit * 100.0
            : 0.0;

        return new BudgetSummaryResponse(
            yearMonth.toString(), totalRequestedAmount, totalBaseLimit, usagePercent
        );
    }

    /**
     * 관리자 조회용: 월별 클럽 전체 스냅샷.
     * <p>스냅샷이 없는 멤버는 <b>저장하지 않고</b> 정책 계산 결과로 transient 엔티티를 조립해 반환.
     * 실제 저장은 관리자가 {@link #adjust} 로 조정할 때만 일어난다.
     */
    public List<MemberMonthlyBudget> listForMonth(Long clubId, List<Member> activeMembers, YearMonth yearMonth) {
        List<MemberMonthlyBudget> existing = budgetRepository
            .findAllByClubIdAndYearMonth(clubId, yearMonth.toString());
        java.util.Map<Long, MemberMonthlyBudget> byMember = new java.util.HashMap<>();
        for (MemberMonthlyBudget b : existing) byMember.put(b.getMemberId(), b);

        List<MemberMonthlyBudget> result = new java.util.ArrayList<>();
        for (Member m : activeMembers) {
            MemberMonthlyBudget hit = byMember.get(m.getId());
            if (hit != null) {
                result.add(hit);
            } else {
                int limit = budgetPolicy.computeBaseLimit(m, yearMonth);
                // transient (not persisted) — save 하지 않는다. 관리자가 adjust 눌러야 DB row 가 생김.
                result.add(MemberMonthlyBudget.of(clubId, m.getId(), yearMonth, limit));
            }
        }
        return result;
    }
}
