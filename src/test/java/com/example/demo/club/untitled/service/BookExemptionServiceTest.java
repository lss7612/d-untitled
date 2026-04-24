package com.example.demo.club.untitled.service;

import com.example.demo.club.service.ClubService;
import com.example.demo.club.untitled.domain.Book;
import com.example.demo.club.untitled.domain.BookExemptionRequest;
import com.example.demo.club.untitled.domain.BookExemptionRequest.SourceType;
import com.example.demo.club.untitled.domain.BookExemptionRequest.Status;
import com.example.demo.club.untitled.dto.BookExemptionResponse;
import com.example.demo.club.untitled.external.AladinApiClient;
import com.example.demo.club.untitled.external.ParsedBook;
import com.example.demo.club.untitled.repository.BookExemptionRequestRepository;
import com.example.demo.club.untitled.repository.BookRepository;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookExemptionServiceTest {

    @Mock
    private BookExemptionRequestRepository exemptionRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ClubService clubService;
    @Mock
    private AladinApiClient aladinApiClient;

    @InjectMocks
    private BookExemptionService service;

    private Member caller;
    private Member admin;

    private static final Long CLUB_ID = 1L;
    private static final String URL = "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=123";

    @BeforeEach
    void setUp() {
        caller = Member.create("user@kr.doubledown.com", "사용자", null);
        setId(caller, 10L);
        admin = Member.create("admin@kr.doubledown.com", "관리자", null);
        setId(admin, 99L);
    }

    // === requestByUrl ===

    @Test
    @DisplayName("requestByUrl: 카탈로그에 없는 책이면 Book(copies=0) 생성 후 PENDING 저장")
    void requestByUrlCreatesPreentryAndPending() {
        ParsedBook parsed = parsedOf("해피 니팅", "홍길동");
        when(aladinApiClient.parse(URL)).thenReturn(parsed);
        when(bookRepository.findByClubIdAndNormalizedTitleAndNormalizedAuthor(eq(CLUB_ID), any(), any()))
            .thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> {
            Book b = inv.getArgument(0);
            setId(b, 500L);
            return b;
        });
        when(exemptionRepository.findByClubIdAndBookIdAndStatus(CLUB_ID, 500L, Status.PENDING))
            .thenReturn(Optional.empty());
        when(exemptionRepository.save(any(BookExemptionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        BookExemptionResponse res = service.requestByUrl(CLUB_ID, URL, "읽어보고 싶어요", caller);

        assertThat(res.bookId()).isEqualTo(500L);
        assertThat(res.status()).isEqualTo("PENDING");
        assertThat(res.sourceType()).isEqualTo("USER_REQUEST");

        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(bookCaptor.capture());
        assertThat(bookCaptor.getValue().getCopies()).isZero();
    }

    @Test
    @DisplayName("requestByUrl: 이미 exempt 인 책이면 400")
    void requestByUrlBlocksWhenAlreadyExempt() {
        ParsedBook parsed = parsedOf("해피 니팅", "홍길동");
        when(aladinApiClient.parse(URL)).thenReturn(parsed);
        Book existing = Book.ofCatalogPreentry(CLUB_ID, "해피 니팅", "홍길동", null, URL, null);
        existing.grantExemption();
        when(bookRepository.findByClubIdAndNormalizedTitleAndNormalizedAuthor(eq(CLUB_ID), any(), any()))
            .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.requestByUrl(CLUB_ID, URL, null, caller))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("이미 제한이 해제된");
        verify(exemptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("requestByUrl: 기존 PENDING 이 있으면 409")
    void requestByUrlBlocksWhenPendingExists() {
        ParsedBook parsed = parsedOf("해피 니팅", "홍길동");
        when(aladinApiClient.parse(URL)).thenReturn(parsed);
        Book existing = Book.ofCatalogPreentry(CLUB_ID, "해피 니팅", "홍길동", null, URL, null);
        setId(existing, 500L);
        when(bookRepository.findByClubIdAndNormalizedTitleAndNormalizedAuthor(eq(CLUB_ID), any(), any()))
            .thenReturn(Optional.of(existing));
        when(exemptionRepository.findByClubIdAndBookIdAndStatus(CLUB_ID, 500L, Status.PENDING))
            .thenReturn(Optional.of(BookExemptionRequest.of(CLUB_ID, 500L, 7L, null)));

        assertThatThrownBy(() -> service.requestByUrl(CLUB_ID, URL, null, caller))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("대기 중");
        verify(exemptionRepository, never()).save(any());
    }

    // === proactiveExemptByUrl ===

    @Test
    @DisplayName("proactiveExemptByUrl: 신규 Book 생성 + grantExemption + APPROVED 이력")
    void proactiveExemptByUrlCreatesBookAndApproves() {
        ParsedBook parsed = parsedOf("해피 니팅", "홍길동");
        when(aladinApiClient.parse(URL)).thenReturn(parsed);
        when(bookRepository.findByClubIdAndNormalizedTitleAndNormalizedAuthor(eq(CLUB_ID), any(), any()))
            .thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> {
            Book b = inv.getArgument(0);
            setId(b, 501L);
            return b;
        });
        when(exemptionRepository.save(any(BookExemptionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        BookExemptionResponse res = service.proactiveExemptByUrl(CLUB_ID, URL, "admin 선제 해제", admin);

        assertThat(res.bookId()).isEqualTo(501L);
        assertThat(res.status()).isEqualTo("APPROVED");
        assertThat(res.sourceType()).isEqualTo("ADMIN_PROACTIVE");
        assertThat(res.memberId()).isEqualTo(99L);

        ArgumentCaptor<BookExemptionRequest> exCaptor = ArgumentCaptor.forClass(BookExemptionRequest.class);
        verify(exemptionRepository).save(exCaptor.capture());
        BookExemptionRequest saved = exCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(Status.APPROVED);
        assertThat(saved.getSourceType()).isEqualTo(SourceType.ADMIN_PROACTIVE);
        assertThat(saved.getProcessedByMemberId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("proactiveExemptByUrl: 이미 exempt 인 책은 새 이력 생성 안 함 (idempotent)")
    void proactiveExemptByUrlIdempotentWhenAlreadyExempt() {
        ParsedBook parsed = parsedOf("해피 니팅", "홍길동");
        when(aladinApiClient.parse(URL)).thenReturn(parsed);
        Book existing = Book.ofCatalogPreentry(CLUB_ID, "해피 니팅", "홍길동", null, URL, null);
        setId(existing, 500L);
        existing.grantExemption();
        when(bookRepository.findByClubIdAndNormalizedTitleAndNormalizedAuthor(eq(CLUB_ID), any(), any()))
            .thenReturn(Optional.of(existing));
        BookExemptionRequest prior = BookExemptionRequest.ofAdminProactive(CLUB_ID, 500L, admin.getId(), "prior");
        setId(prior, 77L);
        when(exemptionRepository.findTopByClubIdAndBookIdAndStatusOrderByCreatedAtDesc(CLUB_ID, 500L, Status.APPROVED))
            .thenReturn(Optional.of(prior));
        when(memberRepository.findById(anyLong())).thenReturn(Optional.of(admin));

        BookExemptionResponse res = service.proactiveExemptByUrl(CLUB_ID, URL, "다시 호출", admin);

        assertThat(res.id()).isEqualTo(77L);
        assertThat(res.status()).isEqualTo("APPROVED");
        verify(exemptionRepository, never()).save(any());
        verify(bookRepository, never()).save(any());
    }

    private static ParsedBook parsedOf(String title, String author) {
        return new ParsedBook(
            title, author, "출판사", "9788901234567",
            new BigDecimal("15000"), "KRW", null, URL, null
        );
    }

    /** 엔티티 id 를 리플렉션으로 세팅 (테스트 전용). */
    private static void setId(Object target, Long id) {
        try {
            Field f = findField(target.getClass(), "id");
            f.setAccessible(true);
            f.set(target, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> c = clazz;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
