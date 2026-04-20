package com.example.demo.club.repository;

import com.example.demo.club.domain.ClubMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {
    List<ClubMember> findAllByMemberId(Long memberId);
    Optional<ClubMember> findByClubIdAndMemberId(Long clubId, Long memberId);
    boolean existsByClubIdAndMemberId(Long clubId, Long memberId);
}
