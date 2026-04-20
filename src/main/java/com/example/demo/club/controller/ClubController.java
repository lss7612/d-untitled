package com.example.demo.club.controller;

import com.example.demo.club.dto.ClubResponse;
import com.example.demo.club.service.ClubService;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.user.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @GetMapping
    public ApiResponse<List<ClubResponse>> list(@AuthenticationPrincipal Member member) {
        return ApiResponse.ok(clubService.findAll(member.getId()));
    }

    @GetMapping("/my")
    public ApiResponse<List<ClubResponse>> myClubs(@AuthenticationPrincipal Member member) {
        return ApiResponse.ok(clubService.findMine(member.getId()));
    }
}
