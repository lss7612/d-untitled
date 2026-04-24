package com.example.demo.club.untitled.budget.controller;

import com.example.demo.club.domain.ClubMember;
import com.example.demo.club.domain.ClubMember.MembershipStatus;
import com.example.demo.club.repository.ClubMemberRepository;
import com.example.demo.club.service.ClubService;
import com.example.demo.club.untitled.budget.domain.MemberMonthlyBudget;
import com.example.demo.club.untitled.budget.dto.AdjustBudgetRequest;
import com.example.demo.club.untitled.budget.dto.BudgetSummaryResponse;
import com.example.demo.club.untitled.budget.dto.MemberBudgetResponse;
import com.example.demo.club.untitled.budget.service.MemberBudgetService;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 관리자 전용: 월별 멤버 예산 스냅샷 조회 / 조정.
 * DEVELOPER 는 {@link ClubService#requireAdmin(Long, Long, Member)} 에서 bypass.
 */
@RestController
@RequestMapping("/api/v1/admin/clubs/{clubId}/budgets")
@RequiredArgsConstructor
public class AdminBudgetController {

    private final MemberBudgetService memberBudgetService;
    private final ClubMemberRepository clubMemberRepository;
    private final MemberRepository memberRepository;
    private final ClubService clubService;

    /** 해당 월 ACTIVE 멤버 전원의 스냅샷 (없으면 lazy 생성). */
    @GetMapping
    public ApiResponse<List<MemberBudgetResponse>> list(
        @PathVariable Long clubId,
        @RequestParam String yearMonth,
        @AuthenticationPrincipal Member caller
    ) {
        clubService.requireAdmin(clubId, caller.getId(), caller);
        YearMonth ym = YearMonth.parse(yearMonth);

        List<ClubMember> actives = clubMemberRepository.findAllByClubIdAndStatus(clubId, MembershipStatus.ACTIVE);
        List<Long> memberIds = actives.stream().map(ClubMember::getMemberId).toList();
        Map<Long, Member> memberById = memberRepository.findAllById(memberIds).stream()
            .collect(Collectors.toMap(Member::getId, m -> m));
        List<Member> members = memberIds.stream().map(memberById::get).filter(java.util.Objects::nonNull).toList();

        List<MemberMonthlyBudget> snapshots = memberBudgetService.listForMonth(clubId, members, ym);

        List<MemberBudgetResponse> result = snapshots.stream()
            .map(b -> MemberBudgetResponse.of(b, memberById.get(b.getMemberId())))
            .toList();
        return ApiResponse.ok(result);
    }

    /** 월별 예산 사용 현황 요약: 신청된 예산 / 전체 baseLimit / 사용률. */
    @GetMapping("/summary")
    public ApiResponse<BudgetSummaryResponse> summary(
        @PathVariable Long clubId,
        @RequestParam(required = false) String yearMonth,
        @AuthenticationPrincipal Member caller
    ) {
        YearMonth ym = (yearMonth == null || yearMonth.isBlank()) ? YearMonth.now() : YearMonth.parse(yearMonth);
        return ApiResponse.ok(memberBudgetService.summaryForAdmin(clubId, caller.getId(), ym, caller));
    }

    /** 특정 멤버 한도 조정. */
    @PatchMapping("/{memberId}")
    public ApiResponse<MemberBudgetResponse> adjust(
        @PathVariable Long clubId,
        @PathVariable Long memberId,
        @RequestParam String yearMonth,
        @RequestBody AdjustBudgetRequest req,
        @AuthenticationPrincipal Member caller
    ) {
        YearMonth ym = YearMonth.parse(yearMonth);
        MemberMonthlyBudget budget = memberBudgetService.adjust(
            clubId, memberId, ym, req.baseLimit(), req.reason(), caller
        );
        Member target = memberRepository.findById(memberId).orElse(null);
        return ApiResponse.ok(MemberBudgetResponse.of(budget, target));
    }
}
