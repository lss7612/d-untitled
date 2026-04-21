package com.example.demo.club.untitled.repository;

import com.example.demo.club.untitled.domain.BookReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookReportRepository extends JpaRepository<BookReport, Long> {
    Optional<BookReport> findByClubIdAndMemberIdAndTargetMonth(
        Long clubId, Long memberId, String targetMonth);
    List<BookReport> findAllByClubIdAndTargetMonthOrderBySubmittedAtDesc(
        Long clubId, String targetMonth);
}
