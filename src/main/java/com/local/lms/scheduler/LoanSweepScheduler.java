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

    private static final int BATCH_SIZE = 200;

    private final LoanService loanService;
    private final NotificationService notificationService;

    /**
     * Daily sweep: marks open loans as OVERDUE and applies late fees.
     * Every instance competes for unclaimed loans via FOR UPDATE SKIP LOCKED.
     */
    @Scheduled(cron = "${scheduler.sweep.cron:0 0 1 * * *}")
    public void sweepOverdueLoans() {
        log.info("=== Starting overdue loan sweep (batch size={}) ===", BATCH_SIZE);
        int total = 0;
        int count;
        while ((count = loanService.processOverdueBatch(BATCH_SIZE)) > 0) {
            total += count;
            log.info("Overdue sweep: processed {} loans this batch", count);
        }
        log.info("=== Overdue loan sweep complete — processed {} loans ===", total);
    }

    /**
     * Daily: apply daily fees on active loans that have a DAILY_FEE product config.
     */
    @Scheduled(cron = "${scheduler.sweep.cron:0 0 1 * * *}")
    public void applyDailyFees() {
        log.info("=== Applying daily fees (batch size={}) ===", BATCH_SIZE);
        int total = 0;
        int count;
        while ((count = loanService.applyDailyFeesBatch(BATCH_SIZE)) > 0) {
            total += count;
            log.info("Daily fees: processed {} loans this batch", count);
        }
        log.info("=== Daily fees complete — processed {} loans ===", total);
    }

    /**
     * Daily morning reminder for loans due in the next 3 days.
     */
    @Scheduled(cron = "${scheduler.reminder.cron:0 0 8 * * *}")
    public void sendDueDateReminders() {
        log.info("=== Sending due date reminders (batch size={}) ===", BATCH_SIZE);
        int total = 0;
        int count;
        while ((count = notificationService.sendDueDateReminderBatch(3, BATCH_SIZE)) > 0) {
            total += count;
            log.info("Due date reminders: processed {} loans this batch", count);
        }
        log.info("=== Due date reminders complete — processed {} loans ===", total);
    }
}
