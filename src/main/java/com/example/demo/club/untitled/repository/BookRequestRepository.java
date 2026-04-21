package com.example.demo.club.untitled.repository;

import com.example.demo.club.untitled.domain.BookRequest;
import com.example.demo.club.untitled.domain.BookRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookRequestRepository extends JpaRepository<BookRequest, Long> {

    List<BookRequest> findAllByClubIdAndTargetMonthOrderByCreatedAtDesc(Long clubId, String targetMonth);

    List<BookRequest> findAllByMemberIdAndClubIdAndTargetMonthOrderByCreatedAtDesc(
        Long memberId, Long clubId, String targetMonth);

    Optional<BookRequest> findByMemberIdAndClubIdAndTargetMonthAndIsbn(
        Long memberId, Long clubId, String targetMonth, String isbn);

    boolean existsByClubIdAndTargetMonthAndStatus(Long clubId, String targetMonth, BookRequestStatus status);

    boolean existsByClubIdAndTargetMonthAndStatusIn(Long clubId, String targetMonth, java.util.Collection<BookRequestStatus> statuses);

    List<BookRequest> findAllByClubIdAndTargetMonthAndStatus(Long clubId, String targetMonth, BookRequestStatus status);
}
