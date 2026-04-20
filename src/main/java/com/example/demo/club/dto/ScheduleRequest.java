package com.example.demo.club.dto;

import java.time.LocalDate;

public record ScheduleRequest(
    String typeCode,
    LocalDate date,
    String description
) {}
