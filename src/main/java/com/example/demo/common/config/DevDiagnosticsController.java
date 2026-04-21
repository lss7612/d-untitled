package com.example.demo.common.config;

import com.example.demo.club.repository.ClubMemberRepository;
import com.example.demo.club.repository.ClubRepository;
import com.example.demo.club.untitled.repository.BookReportRepository;
import com.example.demo.club.untitled.repository.BookRequestRepository;
import com.example.demo.club.untitled.repository.OrderItemRepository;
import com.example.demo.club.untitled.repository.OrderRepository;
import com.example.demo.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Dev 진단 API. /api/v1/auth/** 권한이 permitAll이라 인증 없이 접근 가능.
 * 운영 배포 전 제거 권장.
 */
@RestController
@RequestMapping("/api/v1/auth/_dev")
@RequiredArgsConstructor
public class DevDiagnosticsController {

    private final MemberRepository memberRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final BookRequestRepository bookRequestRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookReportRepository bookReportRepository;

    @GetMapping("/counts")
    public Map<String, Long> counts() {
        return Map.of(
            "members", memberRepository.count(),
            "clubs", clubRepository.count(),
            "clubMembers", clubMemberRepository.count(),
            "bookRequests", bookRequestRepository.count(),
            "orders", orderRepository.count(),
            "orderItems", orderItemRepository.count(),
            "bookReports", bookReportRepository.count()
        );
    }
}
