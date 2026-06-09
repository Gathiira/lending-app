package com.local.lms.scheduler;

import com.local.lms.service.LoanService;
import com.local.lms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanSweepScheduler {

    private final LoanService loanService;
    private final NotificationService notificationService;

    /**
     * Daily sweep: marks open loans as OVERDUE and applies late fees.
     */
    @Scheduled(cron = "${scheduler.sweep.cron:0 0 1 * * *}")
    public void sweepOverdueLoans() {
        log.info("=== Starting overdue loan sweep ===");
        try {
            loanService.processOverdueLoans();
        } catch (Exception e) {
            log.error("Error during overdue loan sweep", e);
        }
        log.info("=== Overdue loan sweep complete ===");
    }

    /**
     * Daily: apply daily fees on active loans that have a DAILY_FEE product config.
     */
    @Scheduled(cron = "${scheduler.sweep.cron:0 0 1 * * *}")
    public void applyDailyFees() {
        log.info("=== Applying daily fees ===");
        try {
            loanService.applyDailyFees();
        } catch (Exception e) {
            log.error("Error applying daily fees", e);
        }
    }

    /**
     * Daily morning reminder for loans due in the next 3 days.
     */
    @Scheduled(cron = "${scheduler.reminder.cron:0 0 8 * * *}")
    public void sendDueDateReminders() {
        log.info("=== Sending due date reminders ===");
        try {
            notificationService.sendDueDateReminders(3);
        } catch (Exception e) {
            log.error("Error sending due date reminders", e);
        }
    }
}
