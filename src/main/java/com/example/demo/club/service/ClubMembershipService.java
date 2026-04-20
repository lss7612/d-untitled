package com.example.demo.club.service;

import com.example.demo.club.domain.Club;
import com.example.demo.club.domain.ClubMember;
import com.example.demo.club.domain.ClubMember.ClubRole;
import com.example.demo.club.repository.ClubMemberRepository;
import com.example.demo.club.repository.ClubRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubMembershipService {

    public static final String DEFAULT_CLUB_NAME = "무제";

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;

    @Transactional
    public void autoEnrollDefaultClub(Long memberId) {
        Club defaultClub = clubRepository.findByName(DEFAULT_CLUB_NAME).orElse(null);
        if (defaultClub == null) {
            log.warn("[ClubMembership] 기본 동호회({})가 존재하지 않습니다. 자동 가입 건너뜁니다.", DEFAULT_CLUB_NAME);
            return;
        }
        if (clubMemberRepository.existsByClubIdAndMemberId(defaultClub.getId(), memberId)) {
            return;
        }
        clubMemberRepository.save(ClubMember.of(defaultClub.getId(), memberId, ClubRole.MEMBER));
        log.info("[ClubMembership] 회원 {}을(를) {} 동호회에 자동 가입시켰습니다.", memberId, DEFAULT_CLUB_NAME);
    }
}
