package com.example.demo.club.controller;

import com.example.demo.club.domain.ClubMember;
import com.example.demo.club.dto.ChangeClubRoleRequest;
import com.example.demo.club.dto.ClubMemberResponse;
import com.example.demo.club.dto.JoinRequestResponse;
import com.example.demo.club.service.ClubMembershipService;
import com.example.demo.club.service.ClubService;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/clubs")
@RequiredArgsConstructor
public class AdminClubMembershipController {

    private final ClubService clubService;
    private final ClubMembershipService clubMembershipService;
    private final MemberRepository memberRepository;

    /** 관리자: 해당 동호회 가입 신청(PENDING) 목록. */
    @GetMapping("/{clubId}/join-requests")
    public ApiResponse<List<JoinRequestResponse>> list(
        @PathVariable Long clubId,
        @AuthenticationPrincipal Member caller
    ) {
        clubService.requireAdmin(clubId, caller.getId(), caller);
        List<ClubMember> pending = clubMembershipService.listPendingRequests(clubId);
        Map<Long, Member> memberById = memberRepository.findAllById(
            pending.stream().map(ClubMember::getMemberId).toList()
        ).stream().collect(Collectors.toMap(Member::getId, m -> m));
        List<JoinRequestResponse> result = pending.stream()
            .map(cm -> JoinRequestResponse.of(cm, memberById.get(cm.getMemberId())))
            .toList();
        return ApiResponse.ok(result);
    }

    /** 관리자: 가입 승인. */
    @PostMapping("/{clubId}/join-requests/{memberId}/approve")
    public ApiResponse<JoinRequestResponse> approve(
        @PathVariable Long clubId,
        @PathVariable Long memberId,
        @AuthenticationPrincipal Member caller
    ) {
        clubService.requireAdmin(clubId, caller.getId(), caller);
        ClubMember cm = clubMembershipService.approve(clubId, memberId);
        Member m = memberRepository.findById(memberId).orElse(null);
        return ApiResponse.ok(JoinRequestResponse.of(cm, m));
    }

    /** 관리자: 가입 거절. */
    @PostMapping("/{clubId}/join-requests/{memberId}/reject")
    public ApiResponse<JoinRequestResponse> reject(
        @PathVariable Long clubId,
        @PathVariable Long memberId,
        @AuthenticationPrincipal Member caller
    ) {
        clubService.requireAdmin(clubId, caller.getId(), caller);
        ClubMember cm = clubMembershipService.reject(clubId, memberId);
        Member m = memberRepository.findById(memberId).orElse(null);
        return ApiResponse.ok(JoinRequestResponse.of(cm, m));
    }

    /** 관리자: 해당 동호회의 ACTIVE 멤버 목록. (관리자 지정/해제 UI 에서 사용) */
    @GetMapping("/{clubId}/members")
    public ApiResponse<List<ClubMemberResponse>> listMembers(
        @PathVariable Long clubId,
        @AuthenticationPrincipal Member caller
    ) {
        clubService.requireAdmin(clubId, caller.getId(), caller);
        List<ClubMember> members = clubMembershipService.listActiveMembers(clubId);
        Map<Long, Member> memberById = memberRepository.findAllById(
            members.stream().map(ClubMember::getMemberId).toList()
        ).stream().collect(Collectors.toMap(Member::getId, m -> m));
        List<ClubMemberResponse> result = members.stream()
            .map(cm -> ClubMemberResponse.of(cm, memberById.get(cm.getMemberId())))
            .toList();
        return ApiResponse.ok(result);
    }

    /** 관리자: 특정 멤버의 ClubRole 변경 (ADMIN ↔ MEMBER). */
    @PatchMapping("/{clubId}/members/{memberId}/role")
    public ApiResponse<ClubMemberResponse> changeRole(
        @PathVariable Long clubId,
        @PathVariable Long memberId,
        @RequestBody ChangeClubRoleRequest request,
        @AuthenticationPrincipal Member caller
    ) {
        ClubMember updated = clubMembershipService.changeClubRole(clubId, memberId, request.role(), caller);
        Member m = memberRepository.findById(memberId).orElse(null);
        return ApiResponse.ok(ClubMemberResponse.of(updated, m));
    }
}
