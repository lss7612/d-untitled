package com.example.demo.auth.security;

import com.example.demo.user.domain.Member;
import com.example.demo.user.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${frontend.url}")
    private String frontendUrl;

    private static final Set<String> ALLOWED_DOMAINS = Set.of(
            "kr.doubledown.com", "afewgoodsoft.com", "doubleugames.com"
    );

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        log.info("[OAuth2SuccessHandler] onAuthenticationSuccess called");
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            String picture = oAuth2User.getAttribute("picture");
            log.info("[OAuth2SuccessHandler] email={}", email);

            String domain = email.substring(email.indexOf('@') + 1);
            if (!ALLOWED_DOMAINS.contains(domain)) {
                log.warn("[OAuth2SuccessHandler] domain not allowed: {}", domain);
                response.sendRedirect(frontendUrl + "/login?error=domain_not_allowed");
                return;
            }

            Member member = memberService.upsert(email, name, picture);
            log.info("[OAuth2SuccessHandler] member upserted, id={}", member.getId());

            String token = jwtTokenProvider.generateToken(member.getId());
            log.info("[OAuth2SuccessHandler] token generated, redirecting to frontend");

            String redirectUrl = frontendUrl + "/auth/callback?token=" + token;
            log.info("[OAuth2SuccessHandler] redirectUrl={}", redirectUrl);
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("[OAuth2SuccessHandler] Exception during success handling", e);
            response.sendRedirect(frontendUrl + "/login?error=server_error");
        }
    }
}
