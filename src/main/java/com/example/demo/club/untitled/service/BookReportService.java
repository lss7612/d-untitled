package com.example.demo.club.untitled.service;

import com.example.demo.club.domain.ClubMember;
import com.example.demo.club.repository.ClubMemberRepository;
import com.example.demo.club.repository.ScheduleRepository;
import com.example.demo.club.service.ClubService;
import com.example.demo.club.untitled.domain.BookReport;
import com.example.demo.club.untitled.domain.BookRequest;
import com.example.demo.club.untitled.dto.BookReportRequest;
import com.example.demo.club.untitled.dto.BookReportResponse;
import com.example.demo.club.untitled.dto.MissingSubmittersResponse;
import com.example.demo.club.untitled.dto.MyBookReportsResponse;
import com.example.demo.club.untitled.repository.BookReportRepository;
import com.example.demo.club.untitled.repository.BookRequestRepository;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookReportService {

    public static final int MIN_CONTENT_LENGTH = 50;
    public static final String DEADLINE_TYPE_CODE = "BOOK_REPORT_DEADLINE";

    private final BookReportRepository bookReportRepository;
    private final BookRequestRepository bookRequestRepository;
    private final ScheduleRepository scheduleRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final MemberRepository memberRepository;
    private final ClubService clubService;

    public MyBookReportsResponse findMine(Long clubId, Long memberId, YearMonth targetMonth) {
        clubService.requireMembership(clubId, memberId);

        BookReportResponse report = bookReportRepository
            .findByClubIdAndMemberIdAndTargetMonth(clubId, memberId, targetMonth.toString())
            .map(BookReportResponse::from)
            .orElse(null);

        List<MyBookReportsResponse.BookRequestSummary> myReqs = bookRequestRepository
            .findAllByMemberIdAndClubIdAndTargetMonthOrderByCreatedAtDesc(memberId, clubId, targetMonth.toString())
            .stream()
            .map(br -> new MyBookReportsResponse.BookRequestSummary(
                br.getId(), br.getTitle(), br.getAuthor(), br.getThumbnailUrl()))
            .toList();

        LocalDate deadline = findDeadline(clubId, targetMonth);
        boolean overdue = deadline != null && LocalDate.now().isAfter(deadline);

        return new MyBookReportsResponse(targetMonth.toString(), deadline, overdue, report, myReqs);
    }

    @Transactional
    public BookReportResponse submit(Long clubId, Long memberId, BookReportRequest req) {
        clubService.requireMembership(clubId, memberId);

        ResolvedBook book = resolveBook(clubId, memberId, req);
        validatePayload(req);

        YearMonth targetMonth = YearMonth.now();
        ensureBeforeDeadline(clubId, targetMonth);

        if (bookReportRepository.findByClubIdAndMemberIdAndTargetMonth(clubId, memberId, targetMonth.toString()).isPresent()) {
            throw new BusinessException("이번 달 독후감을 이미 제출했습니다. 수정 API를 사용하세요.", 400);
        }

        BookReport saved = bookReportRepository.save(BookReport.create(
            book.bookRequestId, clubId, memberId, targetMonth.toString(),
            book.title, book.author, book.thumbnailUrl,
            req.title(), req.content()
        ));
        return BookReportResponse.from(saved);
    }

    @Transactional
    public BookReportResponse update(Long clubId, Long memberId, Long id, BookReportRequest req) {
        clubService.requireMembership(clubId, memberId);

        BookReport report = bookReportRepository.findById(id)
            .orElseThrow(() -> new BusinessException("독후감을 찾을 수 없습니다.", 404));
        if (!report.getMemberId().equals(memberId)) {
            throw new BusinessException("본인 독후감만 수정할 수 있습니다.", 403);
        }
        if (!report.getClubId().equals(clubId)) {
            throw new BusinessException("동호회가 일치하지 않습니다.", 400);
        }

        YearMonth targetMonth = YearMonth.parse(report.getTargetMonth());
        ensureBeforeDeadline(clubId, targetMonth);

        ResolvedBook book = resolveBook(clubId, memberId, req);
        validatePayload(req);

        report.edit(
            book.bookRequestId, book.title, book.author, book.thumbnailUrl,
            req.title(), req.content()
        );
        return BookReportResponse.from(report);
    }

    public List<BookReportResponse> findAllByMonth(Long clubId, Long memberId, YearMonth targetMonth) {
        clubService.requireMembership(clubId, memberId);
        List<BookReport> reports = bookReportRepository
            .findAllByClubIdAndTargetMonthOrderBySubmittedAtDesc(clubId, targetMonth.toString());
        Set<Long> memberIds = reports.stream().map(BookReport::getMemberId).collect(Collectors.toSet());
        Map<Long, Member> memberMap = memberRepository.findAllById(memberIds).stream()
            .collect(Collectors.toMap(Member::getId, m -> m));
        return reports.stream().map(r -> BookReportResponse.of(r, memberMap.get(r.getMemberId()))).toList();
    }

    public MissingSubmittersResponse findMissing(Long clubId, Long adminMemberId, YearMonth targetMonth) {
        clubService.requireAdmin(clubId, adminMemberId);

        List<ClubMember> members = clubMemberRepository.findAllByClubId(clubId);
        Set<Long> memberIds = members.stream().map(ClubMember::getMemberId).collect(Collectors.toSet());

        Set<Long> submittedIds = bookReportRepository
            .findAllByClubIdAndTargetMonthOrderBySubmittedAtDesc(clubId, targetMonth.toString())
            .stream().map(BookReport::getMemberId).collect(Collectors.toSet());

        Set<Long> missingIds = memberIds.stream().filter(id -> !submittedIds.contains(id)).collect(Collectors.toSet());
        Map<Long, Member> memberMap = memberRepository.findAllById(missingIds).stream()
            .collect(Collectors.toMap(Member::getId, m -> m));

        List<MissingSubmittersResponse.MissingSubmitter> missing = missingIds.stream()
            .map(id -> {
                Member m = memberMap.get(id);
                return new MissingSubmittersResponse.MissingSubmitter(
                    id, m == null ? null : m.getName(), m == null ? null : m.getEmail()
                );
            })
            .sorted((a, b) -> {
                String an = a.memberName() == null ? "" : a.memberName();
                String bn = b.memberName() == null ? "" : b.memberName();
                return an.compareTo(bn);
            })
            .toList();

        LocalDate deadline = findDeadline(clubId, targetMonth);
        return new MissingSubmittersResponse(
            targetMonth.toString(), deadline,
            members.size(), submittedIds.size(),
            missing
        );
    }

    private record ResolvedBook(Long bookRequestId, String title, String author, String thumbnailUrl) {}

    /** payload에서 도서 정보를 결정. bookRequestId 우선, 없으면 자유 입력. */
    private ResolvedBook resolveBook(Long clubId, Long memberId, BookReportRequest req) {
        if (req.bookRequestId() != null) {
            BookRequest br = bookRequestRepository.findById(req.bookRequestId())
                .orElseThrow(() -> new BusinessException("신청한 도서를 찾을 수 없습니다.", 404));
            if (!br.getMemberId().equals(memberId)) {
                throw new BusinessException("본인이 신청한 도서만 사용할 수 있습니다.", 403);
            }
            if (!br.getClubId().equals(clubId)) {
                throw new BusinessException("동호회가 일치하지 않습니다.", 400);
            }
            return new ResolvedBook(br.getId(), br.getTitle(), br.getAuthor(), br.getThumbnailUrl());
        }
        if (req.bookTitle() == null || req.bookTitle().isBlank()) {
            throw new BusinessException("도서 제목을 입력하거나 신청한 책을 선택해주세요.", 400);
        }
        return new ResolvedBook(null, req.bookTitle().trim(),
            blankToNull(req.bookAuthor()), blankToNull(req.bookThumbnailUrl()));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private void validatePayload(BookReportRequest req) {
        if (req.title() == null || req.title().isBlank()) {
            throw new BusinessException("독후감 제목을 입력하세요.", 400);
        }
        if (req.content() == null || req.content().length() < MIN_CONTENT_LENGTH) {
            throw new BusinessException("본문은 최소 " + MIN_CONTENT_LENGTH + "자 이상 작성해주세요.", 400);
        }
    }

    private LocalDate findDeadline(Long clubId, YearMonth targetMonth) {
        return scheduleRepository
            .findAllByClubIdAndYearMonthValueOrderByDateAsc(clubId, targetMonth.toString())
            .stream()
            .filter(s -> DEADLINE_TYPE_CODE.equals(s.getTypeCode()))
            .map(s -> s.getDate())
            .findFirst()
            .orElse(null);
    }

    private void ensureBeforeDeadline(Long clubId, YearMonth targetMonth) {
        LocalDate deadline = findDeadline(clubId, targetMonth);
        if (deadline != null && LocalDate.now().isAfter(deadline)) {
            throw new BusinessException("독후감 마감일(" + deadline + ")이 지났습니다.", 400);
        }
    }
}
