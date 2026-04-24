package com.example.demo.common.notification;

import com.example.demo.club.untitled.budget.domain.BudgetShare;
import com.example.demo.user.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoggingNotificationService implements NotificationService {

    @Override
    public void onBudgetShareRequested(BudgetShare share, Member requester, Member sender) {
        log.info("[Notify] BudgetShare requested — id={} club={} month={} requester='{}'({}) -> sender='{}'({}) amount={} note='{}'",
            share.getId(), share.getClubId(), share.getTargetMonth(),
            name(requester), requester != null ? requester.getId() : null,
            name(sender), sender != null ? sender.getId() : null,
            share.getAmount(), share.getNote());
    }

    @Override
    public void onBudgetShareAccepted(BudgetShare share, Member requester, Member sender) {
        log.info("[Notify] BudgetShare accepted — id={} club={} month={} requester='{}' sender='{}' amount={}",
            share.getId(), share.getClubId(), share.getTargetMonth(),
            name(requester), name(sender), share.getAmount());
    }

    private String name(Member m) {
        return m == null ? "?" : m.getName();
    }
}
