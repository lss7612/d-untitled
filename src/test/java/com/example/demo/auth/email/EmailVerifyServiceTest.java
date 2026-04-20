package com.example.demo.auth.email;

import com.example.demo.auth.security.JwtTokenProvider;
import com.example.demo.club.service.ClubMembershipService;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerifyServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ClubMembershipService clubMembershipService;

    private VerifyCodeStore verifyCodeStore;
    private EmailVerifyService emailVerifyService;

    @BeforeEach
    void setUp() {
        verifyCodeStore = new VerifyCodeStore();
        emailVerifyService = new EmailVerifyService(verifyCodeStore, mailSender, memberRepository, jwtTokenProvider, clubMembershipService);
    }

    // === sendCode 테스트 ===

    @Test
    @DisplayName("6자리 숫자 코드를 생성한다")
    void generateCode() {
        String code = emailVerifyService.generateCode();

        assertThat(code).hasSize(6);
        assertThat(code).matches("\\d{6}");
    }

    @Test
    @DisplayName("생성된 코드는 매번 다르다 (SecureRandom)")
    void generateCodeIsRandom() {
        String code1 = emailVerifyService.generateCode();

        boolean allSame = true;
        for (int i = 0; i < 100; i++) {
            if (!emailVerifyService.generateCode().equals(code1)) {
                allSame = false;
                break;
            }
        }
        assertThat(allSame).isFalse();
    }

    @Test
    @DisplayName("sendCode 호출 시 코드가 저장소에 저장된다")
    void sendCodeSavesCode() {
        String email = "user@kr.doubledown.com";

        emailVerifyService.sendCode(email);

        assertThat(verifyCodeStore.find(email)).isPresent();
        assertThat(verifyCodeStore.find(email).get()).matches("\\d{6}");
    }

    @Test
    @DisplayName("sendCode 호출 시 이메일이 발송된다")
    void sendCodeSendsEmail() {
        String email = "user@kr.doubledown.com";

        emailVerifyService.sendCode(email);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly(email);
        assertThat(sent.getSubject()).contains("인증 코드");
        assertThat(sent.getText()).contains("인증 코드:");
    }

    // === verifyCode 테스트 ===

    @Test
    @DisplayName("올바른 코드 입력 시 검증 성공하고 JWT 토큰을 반환한다")
    void verifyCodeSuccess() {
        String email = "user@kr.doubledown.com";
        verifyCodeStore.save(email, "123456");

        Member member = Member.create(email, "Test User", null);
        when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
        when(jwtTokenProvider.generateToken(any(), eq(true))).thenReturn("new-jwt-token");

        String token = emailVerifyService.verifyCode(email, "123456");

        assertThat(token).isEqualTo("new-jwt-token");
        assertThat(member.isEmailVerified()).isTrue();
        assertThat(verifyCodeStore.find(email)).isEmpty(); // 코드 삭제됨
    }

    @Test
    @DisplayName("잘못된 코드 입력 시 BusinessException 발생")
    void verifyCodeWrongCode() {
        String email = "user@kr.doubledown.com";
        verifyCodeStore.save(email, "123456");

        assertThatThrownBy(() -> emailVerifyService.verifyCode(email, "000000"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("인증 코드가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("코드가 존재하지 않으면 BusinessException 발생")
    void verifyCodeNoCode() {
        assertThatThrownBy(() -> emailVerifyService.verifyCode("user@kr.doubledown.com", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("인증 코드가 존재하지 않거나 만료되었습니다.");
    }

    @Test
    @DisplayName("5회 오입력 시 잠금 상태가 되고 429 에러 발생")
    void verifyCodeLockAfterFiveFailures() {
        String email = "user@kr.doubledown.com";
        verifyCodeStore.save(email, "123456");

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> emailVerifyService.verifyCode(email, "000000"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("일치하지 않습니다");
        }

        // 5번째 실패 → 잠금
        assertThatThrownBy(() -> emailVerifyService.verifyCode(email, "000000"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(429);
                    assertThat(be.getMessage()).contains("잠금");
                });
    }

    @Test
    @DisplayName("잠금 상태에서 올바른 코드를 입력해도 429 에러 발생")
    void verifyCodeLockedEvenWithCorrectCode() {
        String email = "user@kr.doubledown.com";
        verifyCodeStore.save(email, "123456");

        for (int i = 0; i < 5; i++) {
            verifyCodeStore.recordFailure(email);
        }

        assertThatThrownBy(() -> emailVerifyService.verifyCode(email, "123456"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(429);
                });
    }

    @Test
    @DisplayName("잠금 상태에서 sendCode도 차단된다")
    void sendCodeBlockedWhenLocked() {
        String email = "user@kr.doubledown.com";

        for (int i = 0; i < 5; i++) {
            verifyCodeStore.recordFailure(email);
        }

        assertThatThrownBy(() -> emailVerifyService.sendCode(email))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(429);
                });
    }
}
