package com.example.demo.club.service;

import com.example.demo.club.domain.Club;
import com.example.demo.club.domain.ClubMember;
import com.example.demo.club.domain.ClubMember.ClubRole;
import com.example.demo.club.domain.ClubMember.MembershipStatus;
import com.example.demo.club.repository.ClubMemberRepository;
import com.example.demo.club.repository.ClubRepository;
import com.example.demo.club.untitled.budget.service.MemberBudgetService;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubMembershipService {

    public static final String DEFAULT_CLUB_NAME = "무제";

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final MemberRepository memberRepository;
    private final MemberBudgetService memberBudgetService;
    private final ClubService clubService;

    @Value("${app.developer-emails:sslee@kr.doubledown.com}")
    private String developerEmailsRaw;

    private Set<String> developerEmails() {
        if (developerEmailsRaw == null || developerEmailsRaw.isBlank()) return Set.of();
        return Arrays.stream(developerEmailsRaw.split(","))
            .map(String::trim).filter(s -> !s.isBlank())
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
    }

    /**
     * 로그인/이메일 인증 완료 시 호출.
     * - DEVELOPER 이메일: Member.role = DEVELOPER 로 승격 + 모든 동호회에 ClubRole.ADMIN + ACTIVE 자동 가입
     * - 일반 회원: 아무 일도 하지 않음 (동호회 가입 신청은 회원이 명시적으로 수행)
     *
     * 주: 앱 전역 관리자 역할은 더 이상 존재하지 않는다. 클럽 관리자 권한은
     * 반드시 {@link #changeClubRole}/DEVELOPER 자동 가입 경로를 통해 ClubRole.ADMIN 으로 부여된다.
     */
    @Transactional
    public void onAuthenticated(Member member) {
        String emailLower = member.getEmail().toLowerCase();
        boolean isDeveloper = developerEmails().contains(emailLower);

        if (isDeveloper) {
            if (member.getRole() != Member.Role.DEVELOPER) {
                member.assignRole(Member.Role.DEVELOPER);
            }
            ensureDeveloperClubMemberships(member);
        }
    }

    /** DEVELOPER 는 모든 동호회에 ADMIN + ACTIVE 로 자동 가입되어 있도록 보장한다. */
    private void ensureDeveloperClubMemberships(Member member) {
        List<Club> allClubs = clubRepository.findAll();
        for (Club club : allClubs) {
            clubMemberRepository.findByClubIdAndMemberId(club.getId(), member.getId())
                .ifPresentOrElse(
                    cm -> {
                        boolean changed = false;
                        if (cm.getRole() != ClubRole.ADMIN) {
                            cm.changeRole(ClubRole.ADMIN);
                            changed = true;
                        }
                        if (!cm.isActive()) {
                            cm.approve();
                            changed = true;
                        }
                        if (changed) {
                            log.info("[ClubMembership] DEVELOPER {} → {} 동호회 ADMIN/ACTIVE 승격",
                                member.getEmail(), club.getName());
                        }
                        // ACTIVE 상태면 현재월 예산 스냅샷 보장 (월 전환 대응)
                        if (cm.isActive()) {
                            memberBudgetService.getOrCreate(club.getId(), member, YearMonth.now());
                        }
                    },
                    () -> {
                        clubMemberRepository.save(ClubMember.of(
                            club.getId(), member.getId(), ClubRole.ADMIN, MembershipStatus.ACTIVE));
                        log.info("[ClubMembership] DEVELOPER {} 을(를) {} 동호회에 자동 가입",
                            member.getEmail(), club.getName());
                        memberBudgetService.getOrCreate(club.getId(), member, YearMonth.now());
                    }
                );
        }
    }

    /** 회원이 동호회에 가입 신청. */
    @Transactional
    public ClubMember requestJoin(Long clubId, Member member) {
        clubRepository.findById(clubId)
            .orElseThrow(() -> new BusinessException("동호회를 찾을 수 없습니다.", 404));

        return clubMemberRepository.findByClubIdAndMemberId(clubId, member.getId())
            .map(existing -> {
                if (existing.isActive()) {
                    throw new BusinessException("이미 가입된 동호회입니다.", 409);
                }
                if (existing.isPending()) {
                    throw new BusinessException("이미 신청했습니다. 관리자 승인을 기다려주세요.", 409);
                }
                // REJECTED → 재신청 허용
                existing.changeRole(ClubRole.MEMBER);
                ClubMember.class.cast(existing); // keep reference
                // status 를 다시 PENDING 으로 되돌릴 수 있는 메서드 필요
                repending(existing);
                log.info("[ClubMembership] 회원 {} 이(가) {} 동호회 재가입 신청", member.getEmail(), clubId);
                return existing;
            })
            .orElseGet(() -> {
                ClubMember saved = clubMemberRepository.save(ClubMember.requestJoin(clubId, member.getId()));
                log.info("[ClubMembership] 회원 {} 이(가) {} 동호회 가입 신청", member.getEmail(), clubId);
                return saved;
            });
    }

    /** REJECTED → PENDING 으로 재설정 (재신청). */
    private void repending(ClubMember cm) {
        // ClubMember 에 public 메서드를 새로 만들기 보단 약간의 내부 트릭으로 처리.
        // 엔티티 직접 수정이 가장 깔끔하므로 메서드 추가 선호되나, 일단 approve/reject 의 조합으로는 안되므로
        // reflection 대신 새 row 는 unique 제약으로 불가. 따라서 엔티티에 repending() 을 추가해야 함.
        cm.repending();
    }

    /** 관리자: 가입 신청 목록 조회 (PENDING). */
    public List<ClubMember> listPendingRequests(Long clubId) {
        return clubMemberRepository.findAllByClubIdAndStatus(clubId, MembershipStatus.PENDING);
    }

    /** 관리자: 가입 승인. 승인과 동시에 현재월 예산 스냅샷을 생성한다. */
    @Transactional
    public ClubMember approve(Long clubId, Long memberId) {
        ClubMember cm = clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)
            .orElseThrow(() -> new BusinessException("가입 신청을 찾을 수 없습니다.", 404));
        if (!cm.isPending()) {
            throw new BusinessException("대기 중인 신청이 아닙니다.", 400);
        }
        cm.approve();

        Member target = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException("회원 정보를 찾을 수 없습니다.", 404));
        memberBudgetService.getOrCreate(clubId, target, YearMonth.now());

        log.info("[ClubMembership] 가입 승인 clubId={} memberId={}", clubId, memberId);
        return cm;
    }

    /** 관리자: 가입 거절. */
    @Transactional
    public ClubMember reject(Long clubId, Long memberId) {
        ClubMember cm = clubMemberRepository.findByClubIdAndMemberId(clubId, memberId)
            .orElseThrow(() -> new BusinessException("가입 신청을 찾을 수 없습니다.", 404));
        if (!cm.isPending()) {
            throw new BusinessException("대기 중인 신청이 아닙니다.", 400);
        }
        cm.reject();
        log.info("[ClubMembership] 가입 거절 clubId={} memberId={}", clubId, memberId);
        return cm;
    }

    /** 회원 조회 헬퍼. */
    public Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException("회원을 찾을 수 없습니다.", 404));
    }

    /** 관리자: 해당 동호회의 ACTIVE 멤버 전체 목록. (관리자 지정/해제 UI 에서 사용) */
    public List<ClubMember> listActiveMembers(Long clubId) {
        return clubMemberRepository.findAllByClubIdAndStatus(clubId, MembershipStatus.ACTIVE);
    }

    /**
     * 관리자: 특정 회원의 ClubRole 을 변경한다 (ADMIN ↔ MEMBER).
     * - caller 는 해당 클럽의 ADMIN 이거나 DEVELOPER 여야 한다.
     * - 대상 멤버는 ACTIVE 상태여야 한다.
     * - 본인의 ClubRole 은 스스로 변경할 수 없다 (실수로 ADMIN 권한 전부 회수 방지).
     */
    @Transactional
    public ClubMember changeClubRole(Long clubId, Long targetMemberId, ClubRole newRole, Member caller) {
        if (newRole == null) {
            throw new BusinessException("변경할 역할(role)이 필요합니다.", 400);
        }
        clubService.requireAdmin(clubId, caller.getId(), caller);
        if (caller.getId().equals(targetMemberId)) {
            throw new BusinessException("본인의 역할은 스스로 변경할 수 없습니다.", 400);
        }
        ClubMember target = clubMemberRepository.findByClubIdAndMemberId(clubId, targetMemberId)
            .orElseThrow(() -> new BusinessException("가입된 멤버가 아닙니다.", 404));
        if (!target.isActive()) {
            throw new BusinessException("활성 멤버만 역할을 변경할 수 있습니다.", 400);
        }
        if (target.getRole() != newRole) {
            target.changeRole(newRole);
            log.info("[ClubMembership] ClubRole 변경 clubId={} memberId={} -> {}", clubId, targetMemberId, newRole);
        }
        return target;
    }
}
