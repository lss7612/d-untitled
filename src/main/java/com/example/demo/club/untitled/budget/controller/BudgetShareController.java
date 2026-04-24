package com.example.demo.club.untitled.budget.controller;

import com.example.demo.club.untitled.budget.dto.BudgetShareResponse;
import com.example.demo.club.untitled.budget.dto.CreateBudgetShareRequest;
import com.example.demo.club.untitled.budget.dto.ShareCandidateResponse;
import com.example.demo.club.untitled.budget.service.BudgetShareService;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.user.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

/**
 * 유저용 예산 나눔 API.
 * Prefix: /api/v1/clubs/{clubId}/budget-shares
 */
@RestController
@RequestMapping("/api/v1/clubs/{clubId}/budget-shares")
@RequiredArgsConstructor
public class BudgetShareController {

    private final BudgetShareService budgetShareService;

    /** 후보 멤버 리스트 + 각 멤버의 실시간 remaining. */
    @GetMapping("/candidates")
    public ApiResponse<List<ShareCandidateResponse>> candidates(
        @PathVariable Long clubId,
        @RequestParam String yearMonth,
        @AuthenticationPrincipal Member caller
    ) {
        return ApiResponse.ok(budgetShareService.listCandidates(clubId, YearMonth.parse(yearMonth), caller));
    }

    /** 내게 들어온 PENDING (수락 대기). */
    @GetMapping("/incoming")
    public ApiResponse<List<BudgetShareResponse>> incoming(
        @PathVariable Long clubId,
        @RequestParam String yearMonth,
        @AuthenticationPrincipal Member caller
    ) {
        return ApiResponse.ok(budgetShareService.listIncomingPending(clubId, YearMonth.parse(yearMonth), caller));
    }

    /** 내가 보낸 PENDING (취소 가능). */
    @GetMapping("/outgoing")
    public ApiResponse<List<BudgetShareResponse>> outgoing(
        @PathVariable Long clubId,
        @RequestParam String yearMonth,
        @AuthenticationPrincipal Member caller
    ) {
        return ApiResponse.ok(budgetShareService.listOutgoingPending(clubId, YearMonth.parse(yearMonth), caller));
    }

    /** 내가 참여한 ACCEPTED (배너용). */
    @GetMapping("/accepted")
    public ApiResponse<List<BudgetShareResponse>> accepted(
        @PathVariable Long clubId,
        @RequestParam String yearMonth,
        @AuthenticationPrincipal Member caller
    ) {
        return ApiResponse.ok(budgetShareService.listAcceptedInvolving(clubId, YearMonth.parse(yearMonth), caller));
    }

    @PostMapping
    public ApiResponse<BudgetShareResponse> create(
        @PathVariable Long clubId,
        @RequestBody CreateBudgetShareRequest req,
        @AuthenticationPrincipal Member caller
    ) {
        YearMonth ym = YearMonth.parse(req.targetMonth());
        return ApiResponse.ok(budgetShareService.createRequest(
            clubId, ym, caller, req.senderId(), req.amount(), req.note()
        ));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<BudgetShareResponse> accept(
        @PathVariable Long clubId,
        @PathVariable Long id,
        @AuthenticationPrincipal Member caller
    ) {
        return ApiResponse.ok(budgetShareService.accept(clubId, id, caller));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<BudgetShareResponse> reject(
        @PathVariable Long clubId,
        @PathVariable Long id,
        @AuthenticationPrincipal Member caller
    ) {
        return ApiResponse.ok(budgetShareService.reject(clubId, id, caller));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<BudgetShareResponse> cancel(
        @PathVariable Long clubId,
        @PathVariable Long id,
        @AuthenticationPrincipal Member caller
    ) {
        return ApiResponse.ok(budgetShareService.cancel(clubId, id, caller));
    }
}
