package com.example.demo.club.service;

import com.example.demo.club.domain.Schedule;
import com.example.demo.club.dto.ScheduleRequest;
import com.example.demo.club.dto.ScheduleResponse;
import com.example.demo.club.repository.ScheduleRepository;
import com.example.demo.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    public List<ScheduleResponse> findByClub(Long clubId, YearMonth yearMonth) {
        List<Schedule> list = (yearMonth == null)
            ? scheduleRepository.findAllByClubIdOrderByDateAsc(clubId)
            : scheduleRepository.findAllByClubIdAndYearMonthValueOrderByDateAsc(clubId, yearMonth.toString());
        return list.stream().map(ScheduleResponse::from).toList();
    }

    @Transactional
    public ScheduleResponse create(Long clubId, ScheduleRequest req) {
        Schedule s = Schedule.create(clubId, req.typeCode(), req.date(), req.description());
        return ScheduleResponse.from(scheduleRepository.save(s));
    }

    @Transactional
    public ScheduleResponse update(Long clubId, Long id, ScheduleRequest req) {
        Schedule s = scheduleRepository.findById(id)
            .orElseThrow(() -> new BusinessException("일정을 찾을 수 없습니다.", 404));
        if (!s.getClubId().equals(clubId)) {
            throw new BusinessException("동호회 일정이 일치하지 않습니다.", 400);
        }
        s.update(req.typeCode(), req.date(), req.description());
        return ScheduleResponse.from(s);
    }

    @Transactional
    public void delete(Long clubId, Long id) {
        Schedule s = scheduleRepository.findById(id)
            .orElseThrow(() -> new BusinessException("일정을 찾을 수 없습니다.", 404));
        if (!s.getClubId().equals(clubId)) {
            throw new BusinessException("동호회 일정이 일치하지 않습니다.", 400);
        }
        scheduleRepository.delete(s);
    }
}
