package com.example.demo.club.service;

import com.example.demo.club.domain.Club;
import com.example.demo.club.domain.ClubMember;
import com.example.demo.club.domain.ClubMember.ClubRole;
import com.example.demo.club.dto.ClubResponse;
import com.example.demo.club.repository.ClubMemberRepository;
import com.example.demo.club.repository.ClubRepository;
import com.example.demo.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;

    public List<ClubResponse> findAll(Long memberId) {
        Map<Long, ClubRole> myMemberships = clubMemberRepository.findAllByMemberId(memberId).stream()
            .collect(Collectors.toMap(ClubMember::getClubId, ClubMember::getRole));
        return clubRepository.findAll().stream()
            .map(c -> ClubResponse.of(c, myMemberships.get(c.getId())))
            .toList();
    }

    public List<ClubResponse> findMine(Long memberId) {
        List<ClubMember> myMemberships = clubMemberRepository.findAllByMemberId(memberId);
        Map<Long, ClubRole> roleByClub = myMemberships.stream()
            .collect(Collectors.toMap(ClubMember::getClubId, ClubMember::getRole));
        return clubRepository.findAllById(roleByClub.keySet()).stream()
            .map(c -> ClubResponse.of(c, roleByClub.get(c.getId())))
            .toList();
    }

    public Club requireMembership(Long clubId, Long memberId) {
        clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)
            .orElseThrow(() -> new BusinessException("동호회에 가입되지 않았습니다.", 403));
        return clubRepository.findById(clubId)
            .orElseThrow(() -> new BusinessException("동호회를 찾을 수 없습니다.", 404));
    }

    public ClubMember requireAdmin(Long clubId, Long memberId) {
        ClubMember cm = clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)
            .orElseThrow(() -> new BusinessException("동호회에 가입되지 않았습니다.", 403));
        if (cm.getRole() != ClubRole.ADMIN) {
            throw new BusinessException("관리자 권한이 필요합니다.", 403);
        }
        return cm;
    }
}
