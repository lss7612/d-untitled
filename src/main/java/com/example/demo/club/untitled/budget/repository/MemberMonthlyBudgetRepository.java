package com.example.demo.club.untitled.budget.repository;

import com.example.demo.club.untitled.budget.domain.MemberMonthlyBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberMonthlyBudgetRepository extends JpaRepository<MemberMonthlyBudget, Long> {

    Optional<MemberMonthlyBudget> findByClubIdAndMemberIdAndYearMonth(Long clubId, Long memberId, String yearMonth);

    List<MemberMonthlyBudget> findAllByClubIdAndYearMonth(Long clubId, String yearMonth);
}
