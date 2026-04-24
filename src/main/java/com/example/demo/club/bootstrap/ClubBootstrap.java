package com.example.demo.club.bootstrap;

import com.example.demo.club.domain.Club;
import com.example.demo.club.domain.Schedule;
import com.example.demo.club.repository.ClubRepository;
import com.example.demo.club.repository.ScheduleRepository;
import com.example.demo.club.service.ClubMembershipService;
import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ClubBootstrap implements CommandLineRunner {

    private final ClubRepository clubRepository;
    private final ScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;
    private final ClubMembershipService membershipService;

    @Override
    @Transactional
    public void run(String... args) {
        Club muje = clubRepository.findByName(ClubMembershipService.DEFAULT_CLUB_NAME)
            .orElseGet(() -> {
                log.info("[Bootstrap] 무제 동호회 시드 생성");
                return clubRepository.save(Club.create(
                    ClubMembershipService.DEFAULT_CLUB_NAME,
                    "독서 동호회 — 매달 책을 읽고 독후감을 나눕니다.",
                    Club.ClubType.READING
                ));
            });

        seedSchedulesIfMissing(muje.getId());

        // 기존 인증 완료 회원의 역할/DEVELOPER 자동 가입만 재확인. 일반 회원의 무제 가입은
        // 본인이 명시적으로 신청해야 한다. (자동 가입 로직 제거됨)
        memberRepository.findAll().stream()
            .filter(Member::isEmailVerified)
            .forEach(membershipService::onAuthenticated);
    }

    private void seedSchedulesIfMissing(Long clubId) {
        YearMonth thisMonth = YearMonth.now();
        if (!scheduleRepository.findAllByClubIdAndYearMonthValueOrderByDateAsc(clubId, thisMonth.toString()).isEmpty()) {
            return;
        }
        log.info("[Bootstrap] {} 무제 일정 시드 생성", thisMonth);
        scheduleRepository.save(Schedule.create(
            clubId, "BOOK_REQUEST_DEADLINE", LocalDate.of(thisMonth.getYear(), thisMonth.getMonth(), 15), "이번 달 책 신청 마감"
        ));
        scheduleRepository.save(Schedule.create(
            clubId, "BOOK_REPORT_DEADLINE", thisMonth.atEndOfMonth(), "이번 달 독후감 마감"
        ));
    }
}
