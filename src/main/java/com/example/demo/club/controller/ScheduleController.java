package com.example.demo.club.controller;

import com.example.demo.club.dto.ScheduleRequest;
import com.example.demo.club.dto.ScheduleResponse;
import com.example.demo.club.service.ClubService;
import com.example.demo.club.service.ScheduleService;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.user.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ClubService clubService;

    @GetMapping("/clubs/{clubId}/schedules")
    public ApiResponse<List<ScheduleResponse>> list(
        @PathVariable Long clubId,
        @RequestParam(required = false) String yearMonth,
        @AuthenticationPrincipal Member member
    ) {
        clubService.requireMembership(clubId, member.getId());
        YearMonth ym = (yearMonth == null || yearMonth.isBlank()) ? null : YearMonth.parse(yearMonth);
        return ApiResponse.ok(scheduleService.findByClub(clubId, ym));
    }

    @PostMapping("/admin/clubs/{clubId}/schedules")
    public ApiResponse<ScheduleResponse> create(
        @PathVariable Long clubId,
        @RequestBody ScheduleRequest req,
        @AuthenticationPrincipal Member member
    ) {
        clubService.requireAdmin(clubId, member.getId());
        return ApiResponse.ok(scheduleService.create(clubId, req));
    }

    @PatchMapping("/admin/clubs/{clubId}/schedules/{id}")
    public ApiResponse<ScheduleResponse> update(
        @PathVariable Long clubId,
        @PathVariable Long id,
        @RequestBody ScheduleRequest req,
        @AuthenticationPrincipal Member member
    ) {
        clubService.requireAdmin(clubId, member.getId());
        return ApiResponse.ok(scheduleService.update(clubId, id, req));
    }

    @DeleteMapping("/admin/clubs/{clubId}/schedules/{id}")
    public ApiResponse<Void> delete(
        @PathVariable Long clubId,
        @PathVariable Long id,
        @AuthenticationPrincipal Member member
    ) {
        clubService.requireAdmin(clubId, member.getId());
        scheduleService.delete(clubId, id);
        return ApiResponse.ok(null);
    }
}
