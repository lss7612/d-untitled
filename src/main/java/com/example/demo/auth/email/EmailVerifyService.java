package com.example.demo.auth.email;

import com.example.demo.auth.security.JwtTokenProvider;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class EmailVerifyService {

    private final VerifyCodeStore verifyCodeStore;
    private final JavaMailSender mailSender;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    private static final SecureRandom RANDOM = new SecureRandom();

    public void sendCode(String email) {
        if (verifyCodeStore.isLocked(email)) {
            throw new BusinessException("인증 시도가 잠금되었습니다. 5분 후 다시 시도해주세요.", 429);
        }

        String code = generateCode();
        verifyCodeStore.save(email, code);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[무제] 이메일 인증 코드");
        message.setText("인증 코드: " + code + "\n\n이 코드는 5분간 유효합니다.");

        mailSender.send(message);
    }

    @Transactional
    public String verifyCode(String email, String code) {
        if (verifyCodeStore.isLocked(email)) {
            throw new BusinessException("인증 시도가 잠금되었습니다. 5분 후 다시 시도해주세요.", 429);
        }

        String storedCode = verifyCodeStore.find(email)
                .orElseThrow(() -> new BusinessException("인증 코드가 존재하지 않거나 만료되었습니다.", 400));

        if (!storedCode.equals(code)) {
            verifyCodeStore.recordFailure(email);
            if (verifyCodeStore.isLocked(email)) {
                throw new BusinessException("인증 시도 5회 실패로 잠금되었습니다. 5분 후 다시 시도해주세요.", 429);
            }
            throw new BusinessException("인증 코드가 일치하지 않습니다.", 400);
        }

        verifyCodeStore.remove(email);

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("회원 정보를 찾을 수 없습니다.", 404));
        member.verifyEmail();

        return jwtTokenProvider.generateToken(member.getId(), true);
    }

    String generateCode() {
        int code = RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }
}
