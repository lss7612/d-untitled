package com.example.demo.club.untitled.repository;

import com.example.demo.club.untitled.domain.MonthLock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonthLockRepository extends JpaRepository<MonthLock, Long> {

    Optional<MonthLock> findByClubIdAndTargetMonth(Long clubId, String targetMonth);
}
