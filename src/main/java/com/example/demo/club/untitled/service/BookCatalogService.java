package com.example.demo.club.untitled.service;

import com.example.demo.club.service.ClubService;
import com.example.demo.club.untitled.domain.Book;
import com.example.demo.club.untitled.dto.BookResponse;
import com.example.demo.club.untitled.repository.BookRepository;
import com.example.demo.club.untitled.util.BookNames;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 보유 책(Book 카탈로그) 조회/검색 전용 서비스.
 *
 * "제한풀기" (exemption grant/revoke) 쓰기 로직은 {@link BookExemptionService} 에 있다.
 * 본 서비스는 읽기 전용 (카탈로그 검색).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookCatalogService {

    private static final int MAX_RESULTS = 200;

    private final BookRepository bookRepository;
    private final ClubService clubService;

    /**
     * 제목/저자로 보유 책 검색. 둘 다 null/blank 이면 최근 등록 순 전체 반환 (상한 {@value MAX_RESULTS}).
     */
    public List<BookResponse> search(Long clubId, Long callerId, String titleRaw, String authorRaw) {
        clubService.requireMembership(clubId, callerId);

        String titlePattern = toLikePattern(titleRaw);
        String authorPattern = toLikePattern(authorRaw);

        Pageable limit = PageRequest.of(0, MAX_RESULTS);
        List<Book> rows = bookRepository.searchInClub(clubId, titlePattern, authorPattern, limit);
        return rows.stream().map(BookResponse::from).toList();
    }

    /**
     * 사용자 입력을 LIKE 패턴으로 변환.
     * - null/blank → null (쿼리에서 조건 skip)
     * - `%`, `_`, `\` 를 역슬래시로 escape
     * - 앞뒤에 `%` 를 붙여 contains 매칭
     * - {@link BookNames#normalize} 로 NFKC + 공백 collapse + lowercase
     */
    private static String toLikePattern(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String normalized = BookNames.normalize(raw);
        if (normalized.isEmpty()) return null;
        String escaped = normalized
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
