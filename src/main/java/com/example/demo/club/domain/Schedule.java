package com.example.demo.club.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.YearMonth;

@Entity
@Table(name = "schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    /**
     * 일정 타입 코드. 각 ClubPlugin이 지원하는 typeCode set을 SPI로 등록한다.
     * 예: "BOOK_REQUEST_DEADLINE", "BOOK_REPORT_DEADLINE".
     */
    @Column(name = "type_code", nullable = false, length = 50)
    private String typeCode;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 200)
    private String description;

    @Column(name = "year_month_value", nullable = false, length = 7)
    private String yearMonthValue;

    public static Schedule create(Long clubId, String typeCode, LocalDate date, String description) {
        Schedule s = new Schedule();
        s.clubId = clubId;
        s.typeCode = typeCode;
        s.date = date;
        s.description = description;
        s.yearMonthValue = YearMonth.from(date).toString();
        return s;
    }

    public void update(String typeCode, LocalDate date, String description) {
        this.typeCode = typeCode;
        this.date = date;
        this.description = description;
        this.yearMonthValue = YearMonth.from(date).toString();
    }

    public YearMonth getYearMonth() {
        return YearMonth.parse(yearMonthValue);
    }
}
