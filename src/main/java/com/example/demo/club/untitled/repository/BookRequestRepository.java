package com.example.demo.club.untitled.repository;

import com.example.demo.club.untitled.domain.BookRequest;
import com.example.demo.club.untitled.domain.BookRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookRequestRepository extends JpaRepository<BookRequest, Long> {

    List<BookRequest> findAllByClubIdAndTargetMonthOrderByCreatedAtDesc(Long clubId, String targetMonth);

    /** 클럽 전체·특정 월의 모든 BookRequest (상태 무관, 모든 멤버). 타 멤버 중복 신청 탐지용. */
    List<BookRequest> findAllByClubIdAndTargetMonth(Long clubId, String targetMonth);

    List<BookRequest> findAllByMemberIdAndClubIdAndTargetMonthOrderByCreatedAtDesc(
        Long memberId, Long clubId, String targetMonth);

    Optional<BookRequest> findByMemberIdAndClubIdAndTargetMonthAndIsbn(
        Long memberId, Long clubId, String targetMonth, String isbn);

    boolean existsByClubIdAndTargetMonthAndStatus(Long clubId, String targetMonth, BookRequestStatus status);

    boolean existsByClubIdAndTargetMonthAndStatusIn(Long clubId, String targetMonth, java.util.Collection<BookRequestStatus> statuses);

    /** 특정 멤버의 해당 월 책 중 주어진 상태들에 하나라도 해당하는 게 있는지 — 나눔 이체 락 판정용. */
    boolean existsByClubIdAndMemberIdAndTargetMonthAndStatusIn(
        Long clubId, Long memberId, String targetMonth, java.util.Collection<BookRequestStatus> statuses);

    List<BookRequest> findAllByClubIdAndTargetMonthAndStatus(Long clubId, String targetMonth, BookRequestStatus status);
}
