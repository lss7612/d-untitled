package com.example.demo.user.controller;

import com.example.demo.common.response.ApiResponse;
import com.example.demo.user.domain.Member;
import com.example.demo.user.dto.MemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    @GetMapping("/me")
    public ApiResponse<MemberResponse> getMe(@AuthenticationPrincipal Member member) {
        return ApiResponse.ok(MemberResponse.from(member));
    }
}
