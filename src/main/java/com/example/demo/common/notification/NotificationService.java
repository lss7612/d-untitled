package com.example.demo.common.notification;

import com.example.demo.club.untitled.budget.domain.BudgetShare;
import com.example.demo.user.domain.Member;

/**
 * 앱 이벤트 알림 허브. 현재는 로깅 구현만 있음.
 * 향후 Slack Incoming Webhook, 이메일 등을 구현체로 갈아 끼움.
 */
public interface NotificationService {
    void onBudgetShareRequested(BudgetShare share, Member requester, Member sender);
    void onBudgetShareAccepted(BudgetShare share, Member requester, Member sender);
}
