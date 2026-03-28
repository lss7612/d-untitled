package com.example.demo.auth.email;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.user.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/email")
@RequiredArgsConstructor
public class EmailVerifyController {

    private final EmailVerifyService emailVerifyService;

    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Void>> sendCode(@AuthenticationPrincipal Member member) {
        if (member.isEmailVerified()) {
            throw new BusinessException("이미 이메일 인증이 완료되었습니다.", 400);
        }

        emailVerifyService.sendCode(member.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<VerifyCodeResponse>> verify(
            @AuthenticationPrincipal Member member,
            @RequestBody VerifyCodeRequest request) {
        if (member.isEmailVerified()) {
            throw new BusinessException("이미 이메일 인증이 완료되었습니다.", 400);
        }

        String token = emailVerifyService.verifyCode(member.getEmail(), request.code());
        return ResponseEntity.ok(ApiResponse.ok(new VerifyCodeResponse(token)));
    }
}
