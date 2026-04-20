package com.example.demo.club.dto;

import com.example.demo.club.domain.Schedule;

import java.time.LocalDate;

public record ScheduleResponse(
    Long id,
    Long clubId,
    String typeCode,
    LocalDate date,
    String description,
    String yearMonth
) {
    public static ScheduleResponse from(Schedule s) {
        return new ScheduleResponse(
            s.getId(),
            s.getClubId(),
            s.getTypeCode(),
            s.getDate(),
            s.getDescription(),
            s.getYearMonthValue()
        );
    }
}
