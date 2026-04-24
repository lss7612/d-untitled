package com.example.demo.common.appconfig.bootstrap;

import com.example.demo.common.appconfig.domain.AppConfig;
import com.example.demo.common.appconfig.domain.AppConfig.ConfigType;
import com.example.demo.common.appconfig.repository.AppConfigRepository;
import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AppConfigSeeder implements CommandLineRunner {

    private final AppConfigRepository appConfigRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedWhiteList();
        seedSsuMember();
    }

    private void seedWhiteList() {
        if (appConfigRepository.findByType(ConfigType.WHITE_LIST).isPresent()) return;
        appConfigRepository.save(AppConfig.of(ConfigType.WHITE_LIST, "[\"lss7612@gmail.com\"]", null));
        log.info("[AppConfigSeeder] WHITE_LIST 초기 시드 완료");
    }

    private void seedSsuMember() {
        String email = "lss7612@gmail.com";
        if (memberRepository.findByEmail(email).isPresent()) return;
        Member ssu = Member.create(email, "쑤", null);
        ssu.verifyEmail();
        memberRepository.save(ssu);
        log.info("[AppConfigSeeder] 더미 회원 '쑤'({}) 생성 완료", email);
    }
}
