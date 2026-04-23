package com.example.demo.club.controller;

import com.example.demo.club.domain.ClubMember;
import com.example.demo.club.dto.ClubResponse;
import com.example.demo.club.dto.JoinRequestResponse;
import com.example.demo.club.service.ClubMembershipService;
import com.example.demo.club.service.ClubService;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.user.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;
    private final ClubMembershipService clubMembershipService;

    @GetMapping
    public ApiResponse<List<ClubResponse>> list(@AuthenticationPrincipal Member member) {
        return ApiResponse.ok(clubService.findAll(member.getId()));
    }

    @GetMapping("/my")
    public ApiResponse<List<ClubResponse>> myClubs(@AuthenticationPrincipal Member member) {
        return ApiResponse.ok(clubService.findMine(member.getId()));
    }

    /** 회원: 동호회 가입 신청. 결과는 PENDING 상태 ClubMember. */
    @PostMapping("/{clubId}/join-requests")
    public ApiResponse<JoinRequestResponse> requestJoin(
        @PathVariable Long clubId,
        @AuthenticationPrincipal Member member
    ) {
        ClubMember cm = clubMembershipService.requestJoin(clubId, member);
        return ApiResponse.ok(JoinRequestResponse.of(cm, member));
    }
}
