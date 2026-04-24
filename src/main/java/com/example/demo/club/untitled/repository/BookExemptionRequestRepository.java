package com.example.demo.club.untitled.repository;

import com.example.demo.club.untitled.domain.BookExemptionRequest;
import com.example.demo.club.untitled.domain.BookExemptionRequest.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookExemptionRequestRepository extends JpaRepository<BookExemptionRequest, Long> {

    /** 동일 책에 대한 중복 PENDING 방지용. */
    Optional<BookExemptionRequest> findByClubIdAndBookIdAndStatus(Long clubId, Long bookId, Status status);

    List<BookExemptionRequest> findAllByClubIdAndStatusOrderByCreatedAtAsc(Long clubId, Status status);

    /** 관리자 URL 해제에서 이미 exempt 인 책의 최신 APPROVED 이력 조회 (idempotent 응답용). */
    Optional<BookExemptionRequest> findTopByClubIdAndBookIdAndStatusOrderByCreatedAtDesc(
        Long clubId, Long bookId, Status status);
}
