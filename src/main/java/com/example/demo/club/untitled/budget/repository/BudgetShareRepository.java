package com.example.demo.club.untitled.budget.repository;

import com.example.demo.club.untitled.budget.domain.BudgetShare;
import com.example.demo.club.untitled.budget.domain.BudgetShareStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetShareRepository extends JpaRepository<BudgetShare, Long> {

    Optional<BudgetShare> findByClubIdAndTargetMonthAndRequesterIdAndSenderIdAndStatus(
        Long clubId, String targetMonth, Long requesterId, Long senderId, BudgetShareStatus status);

    /** sender=memberId + status=상태 — 내게 들어온 / 처리된 리스트. */
    List<BudgetShare> findAllByClubIdAndTargetMonthAndSenderIdAndStatus(
        Long clubId, String targetMonth, Long senderId, BudgetShareStatus status);

    /** requester=memberId + status=상태 — 내가 보낸 / 처리된 리스트. */
    List<BudgetShare> findAllByClubIdAndTargetMonthAndRequesterIdAndStatus(
        Long clubId, String targetMonth, Long requesterId, BudgetShareStatus status);

    /** 내가 requester 또는 sender 인 ACCEPTED 를 한번에. 배너 표시용. */
    @Query("""
        SELECT bs FROM BudgetShare bs
        WHERE bs.clubId = :clubId
          AND bs.targetMonth = :targetMonth
          AND bs.status = com.example.demo.club.untitled.budget.domain.BudgetShareStatus.ACCEPTED
          AND (bs.requesterId = :memberId OR bs.senderId = :memberId)
        ORDER BY bs.respondedAt DESC
    """)
    List<BudgetShare> findAcceptedInvolving(
        @Param("clubId") Long clubId,
        @Param("targetMonth") String targetMonth,
        @Param("memberId") Long memberId
    );

    @Query("""
        SELECT COALESCE(SUM(bs.amount), 0) FROM BudgetShare bs
        WHERE bs.clubId = :clubId
          AND bs.targetMonth = :targetMonth
          AND bs.requesterId = :memberId
          AND bs.status = com.example.demo.club.untitled.budget.domain.BudgetShareStatus.ACCEPTED
    """)
    int sumAcceptedIn(
        @Param("clubId") Long clubId,
        @Param("targetMonth") String targetMonth,
        @Param("memberId") Long memberId
    );

    @Query("""
        SELECT COALESCE(SUM(bs.amount), 0) FROM BudgetShare bs
        WHERE bs.clubId = :clubId
          AND bs.targetMonth = :targetMonth
          AND bs.senderId = :memberId
          AND bs.status = com.example.demo.club.untitled.budget.domain.BudgetShareStatus.ACCEPTED
    """)
    int sumAcceptedOut(
        @Param("clubId") Long clubId,
        @Param("targetMonth") String targetMonth,
        @Param("memberId") Long memberId
    );
}
