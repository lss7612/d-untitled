package com.example.demo.club.repository;

import com.example.demo.club.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findAllByClubIdAndYearMonthValueOrderByDateAsc(Long clubId, String yearMonthValue);
    List<Schedule> findAllByClubIdOrderByDateAsc(Long clubId);
}
