package com.example.demo.club.untitled.service;

import com.example.demo.club.service.ClubService;
import com.example.demo.club.untitled.domain.MonthLock;
import com.example.demo.club.untitled.repository.MonthLockRepository;
import com.example.demo.user.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

/**
 * 클럽·월 단위 잠금 플래그 서비스.
 *
 * <p>잠금 상태는 {@link MonthLock} 엔티티에 저장되며 {@link com.example.demo.club.untitled.domain.BookRequest}
 * 상태와 독립적이다. 레코드가 없으면 {@code locked=false} 로 간주.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthLockService {

    private final MonthLockRepository monthLockRepository;
    private final ClubService clubService;

    public boolean isLocked(Long clubId, YearMonth ym) {
        return monthLockRepository.findByClubIdAndTargetMonth(clubId, ym.toString())
            .map(MonthLock::isLocked)
            .orElse(false);
    }

    @Transactional
    public boolean lock(Long clubId, Long adminMemberId, YearMonth ym, Member caller) {
        clubService.requireAdmin(clubId, adminMemberId, caller);
        MonthLock ml = monthLockRepository.findByClubIdAndTargetMonth(clubId, ym.toString())
            .orElseGet(() -> monthLockRepository.save(MonthLock.of(clubId, ym.toString())));
        ml.lock();
        log.info("[MonthLock] 잠금 clubId={} month={} by={}", clubId, ym, adminMemberId);
        return ml.isLocked();
    }

    @Transactional
    public boolean unlock(Long clubId, Long adminMemberId, YearMonth ym, Member caller) {
        clubService.requireAdmin(clubId, adminMemberId, caller);
        monthLockRepository.findByClubIdAndTargetMonth(clubId, ym.toString())
            .ifPresent(MonthLock::unlock);
        log.info("[MonthLock] 해제 clubId={} month={} by={}", clubId, ym, adminMemberId);
        return false;
    }
}
