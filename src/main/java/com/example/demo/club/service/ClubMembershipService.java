package com.example.demo.club.service;

import com.example.demo.club.domain.Club;
import com.example.demo.club.domain.ClubMember;
import com.example.demo.club.domain.ClubMember.ClubRole;
import com.example.demo.club.repository.ClubMemberRepository;
import com.example.demo.club.repository.ClubRepository;
import com.example.demo.user.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubMembershipService {

    public static final String DEFAULT_CLUB_NAME = "무제";

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;

    @Value("${app.admin-emails:}")
    private String adminEmailsRaw;

    private Set<String> adminEmails() {
        if (adminEmailsRaw == null || adminEmailsRaw.isBlank()) return Set.of();
        return Arrays.stream(adminEmailsRaw.split(","))
            .map(String::trim).filter(s -> !s.isBlank())
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
    }

    public boolean isConfiguredAdmin(Member member) {
        return adminEmails().contains(member.getEmail().toLowerCase());
    }

    @Transactional
    public void autoEnroll(Member member) {
        Club defaultClub = clubRepository.findByName(DEFAULT_CLUB_NAME).orElse(null);
        if (defaultClub == null) {
            log.warn("[ClubMembership] 기본 동호회({})가 존재하지 않습니다. 자동 가입 건너뜁니다.", DEFAULT_CLUB_NAME);
            return;
        }
        ClubRole desired = isConfiguredAdmin(member) ? ClubRole.ADMIN : ClubRole.MEMBER;
        clubMemberRepository.findByClubIdAndMemberId(defaultClub.getId(), member.getId())
            .ifPresentOrElse(
                cm -> {
                    if (cm.getRole() != desired && desired == ClubRole.ADMIN) {
                        cm.changeRole(ClubRole.ADMIN);
                        log.info("[ClubMembership] 회원 {} → {} 동호회 ADMIN 승격", member.getId(), DEFAULT_CLUB_NAME);
                    }
                },
                () -> {
                    clubMemberRepository.save(ClubMember.of(defaultClub.getId(), member.getId(), desired));
                    log.info("[ClubMembership] 회원 {}({})을(를) {} 동호회에 {}로 가입시켰습니다.",
                        member.getId(), member.getEmail(), DEFAULT_CLUB_NAME, desired);
                }
            );
    }
}
