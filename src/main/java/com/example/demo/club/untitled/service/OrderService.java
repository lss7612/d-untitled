package com.example.demo.club.untitled.service;

import com.example.demo.club.service.ClubService;
import com.example.demo.club.untitled.domain.BookRequest;
import com.example.demo.club.untitled.domain.BookRequestStatus;
import com.example.demo.club.untitled.domain.Order;
import com.example.demo.club.untitled.domain.OrderItem;
import com.example.demo.club.untitled.dto.AdminBookRequestRow;
import com.example.demo.club.untitled.dto.OrderItemResponse;
import com.example.demo.club.untitled.dto.OrderResponse;
import com.example.demo.club.untitled.repository.BookRequestRepository;
import com.example.demo.club.untitled.repository.OrderItemRepository;
import com.example.demo.club.untitled.repository.OrderRepository;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookRequestRepository bookRequestRepository;
    private final MemberRepository memberRepository;
    private final ClubService clubService;
    private final MonthLockService monthLockService;

    public List<AdminBookRequestRow> findAllRequests(Long clubId, Long adminMemberId, YearMonth targetMonth) {
        clubService.requireAdmin(clubId, adminMemberId);
        List<BookRequest> all = bookRequestRepository
            .findAllByClubIdAndTargetMonthOrderByCreatedAtDesc(clubId, targetMonth.toString());
        Set<Long> memberIds = all.stream().map(BookRequest::getMemberId).collect(Collectors.toSet());
        Map<Long, Member> memberMap = memberRepository.findAllById(memberIds).stream()
            .collect(Collectors.toMap(Member::getId, m -> m));
        return all.stream()
            .map(br -> AdminBookRequestRow.of(br, memberMap.get(br.getMemberId())))
            .toList();
    }

    public Optional<OrderResponse> findOrder(Long clubId, Long adminMemberId, YearMonth targetMonth) {
        clubService.requireAdmin(clubId, adminMemberId);
        return orderRepository.findByClubIdAndTargetMonth(clubId, targetMonth.toString())
            .map(this::loadResponse);
    }

    /**
     * 선택한 BookRequest들을 PENDING → ORDERED로 전환.
     * Order가 없으면 자동 생성. ISBN별로 OrderItem 누적.
     *
     * <p>가드: 해당 월이 {@link com.example.demo.club.untitled.domain.MonthLock}
     * 으로 잠겨 있지 않으면 400 — 주문 처리 중 회원이 추가 신청하는 사고 방지.
     */
    @Transactional
    public OrderResponse markOrdered(Long clubId, Long adminMemberId, List<Long> ids) {
        clubService.requireAdmin(clubId, adminMemberId);
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("선택한 책이 없습니다.", 400);
        }

        List<BookRequest> targets = bookRequestRepository.findAllById(ids);
        if (targets.size() != ids.size()) {
            throw new BusinessException("일부 신청을 찾을 수 없습니다.", 404);
        }

        // 검증: 같은 동호회 + 같은 월 + 모두 PENDING
        String targetMonth = null;
        for (BookRequest br : targets) {
            if (!br.getClubId().equals(clubId)) {
                throw new BusinessException("다른 동호회의 신청이 포함되었습니다.", 400);
            }
            if (br.getStatus() != BookRequestStatus.PENDING) {
                throw new BusinessException("신청 대기(PENDING) 상태가 아닌 신청이 포함되었습니다: " + br.getStatus().getLabel(), 400);
            }
            if (targetMonth == null) targetMonth = br.getTargetMonth();
            else if (!targetMonth.equals(br.getTargetMonth())) {
                throw new BusinessException("서로 다른 월의 신청이 섞여 있습니다.", 400);
            }
        }

        // 잠금 가드: 잠긴 상태일 때만 주문 처리 허용.
        if (!monthLockService.isLocked(clubId, YearMonth.parse(targetMonth))) {
            throw new BusinessException("먼저 이번 달 신청을 잠가주세요.", 400);
        }

        // Order 가져오거나 생성
        final String tm = targetMonth;
        Order order = orderRepository.findByClubIdAndTargetMonth(clubId, tm)
            .orElseGet(() -> orderRepository.save(Order.create(clubId, tm, adminMemberId, 0, 0)));

        // 각 책: ISBN으로 OrderItem 찾아서 누적
        for (BookRequest br : targets) {
            OrderItem item = orderItemRepository
                .findByOrderIdAndIsbn(order.getId(), br.getIsbn())
                .orElse(null);
            if (item == null) {
                orderItemRepository.save(OrderItem.create(
                    order.getId(),
                    br.getIsbn(), br.getAladinItemCode(),
                    br.getTitle(), br.getAuthor(),
                    br.getPrice(), 1
                ));
            } else {
                item.increase(1);
            }
            br.assignToOrder(order.getId());
        }

        recomputeOrderTotals(order);
        return loadResponse(order);
    }

    /**
     * 선택한 BookRequest들을 ORDERED → PENDING으로 되돌림 (주문 처리 취소).
     * OrderItem quantity 감소 / 0 되면 삭제. Order의 items 모두 사라지면 Order delete.
     * 잠금 상태 여부와 무관하게 되돌리기는 가능.
     */
    @Transactional
    public Optional<OrderResponse> unmarkOrdered(Long clubId, Long adminMemberId, List<Long> ids) {
        clubService.requireAdmin(clubId, adminMemberId);
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("선택한 책이 없습니다.", 400);
        }

        List<BookRequest> targets = bookRequestRepository.findAllById(ids);
        if (targets.size() != ids.size()) {
            throw new BusinessException("일부 신청을 찾을 수 없습니다.", 404);
        }

        String targetMonth = null;
        Long orderId = null;
        for (BookRequest br : targets) {
            if (!br.getClubId().equals(clubId)) {
                throw new BusinessException("다른 동호회의 신청이 포함되었습니다.", 400);
            }
            if (br.getStatus() != BookRequestStatus.ORDERED) {
                throw new BusinessException("ORDERED 상태가 아닌 신청이 포함되었습니다: " + br.getStatus().getLabel(), 400);
            }
            if (br.getOrderId() == null) {
                throw new BusinessException("주문서에 연결되지 않은 신청입니다.", 400);
            }
            if (targetMonth == null) {
                targetMonth = br.getTargetMonth();
                orderId = br.getOrderId();
            } else if (!targetMonth.equals(br.getTargetMonth()) || !orderId.equals(br.getOrderId())) {
                throw new BusinessException("서로 다른 월/주문서의 신청이 섞여 있습니다.", 400);
            }
        }

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException("주문서를 찾을 수 없습니다.", 404));

        for (BookRequest br : targets) {
            OrderItem item = orderItemRepository
                .findByOrderIdAndIsbn(order.getId(), br.getIsbn())
                .orElseThrow(() -> new BusinessException("주문 항목을 찾을 수 없습니다.", 404));
            item.decrease(1);
            if (item.getQuantity() <= 0) {
                orderItemRepository.delete(item);
            }
            br.revertToPending();
        }

        // 누적 항목 재조회 후 합계 갱신, 비어있으면 Order 삭제
        List<OrderItem> remaining = orderItemRepository.findAllByOrderId(order.getId());
        if (remaining.isEmpty()) {
            orderRepository.delete(order);
            return Optional.empty();
        }
        recomputeOrderTotals(order);
        return Optional.of(loadResponse(order));
    }

    private void recomputeOrderTotals(Order order) {
        List<OrderItem> items = orderItemRepository.findAllByOrderId(order.getId());
        int totalQty = items.stream().mapToInt(OrderItem::getQuantity).sum();
        int totalAmount = items.stream().mapToInt(OrderItem::getSubtotal).sum();
        order.updateTotal(totalAmount, totalQty);
    }

    private OrderResponse loadResponse(Order o) {
        List<OrderItemResponse> items = orderItemRepository.findAllByOrderId(o.getId()).stream()
            .map(OrderItemResponse::from).toList();
        return OrderResponse.of(o, items);
    }
}
