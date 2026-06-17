package com.local.lms.service.impl;

import com.local.lms.domain.entity.*;
import com.local.lms.domain.enums.NotificationChannel;
import com.local.lms.domain.enums.NotificationEventType;
import com.local.lms.domain.enums.NotificationStatus;
import com.local.lms.dto.request.NotificationTemplateRequest;
import com.local.lms.dto.response.NotificationTemplateResponse;
import com.local.lms.event.NotificationEvent;
import com.local.lms.exceptions.ResourceNotFoundException;
import com.local.lms.repository.LoanRepository;
import com.local.lms.repository.NotificationRepository;
import com.local.lms.repository.NotificationTrackerRepository;
import com.local.lms.repository.NotificationTemplateRepository;
import com.local.lms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationTrackerRepository trackerRepository;
    private final LoanRepository loanRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void sendNotification(Customer customer, Loan loan, NotificationEventType event) {
        LocalDate today = LocalDate.now();

        // Check idempotency — skip if already tracked for this loan + event + date
        if (trackerRepository.existsByLoanIdAndEventAndNotificationDate(loan.getId(), event, today)) {
            log.debug("Notification already tracked for loan={} event={} date={}, skipping",
                    loan.getId(), event, today);
            return;
        }

        NotificationChannel channel = customer.getPreferredChannel();

        templateRepository.findByEventAndChannelAndActiveTrue(event, channel).ifPresentOrElse(
                template -> {
                    String message = resolveTemplate(template.getBody(), customer, loan);
                    String subject = template.getSubject() != null
                            ? resolveTemplate(template.getSubject(), customer, loan) : null;
                    String recipient = channel == NotificationChannel.SMS
                            ? customer.getPhoneNumber() : customer.getEmail();

                    // Create tracker: PENDING → PROCESSING → SENT/FAILED
                    NotificationTracker tracker = NotificationTracker.builder()
                            .loan(loan)
                            .event(event)
                            .notificationDate(today)
                            .status(NotificationStatus.PENDING)
                            .build();

                    try {
                        tracker = trackerRepository.save(tracker);
                        tracker.setStatus(NotificationStatus.PROCESSING);
                        trackerRepository.save(tracker);

                        Notification notification = Notification.builder()
                                .customer(customer)
                                .loan(loan)
                                .event(event)
                                .channel(channel)
                                .recipient(recipient)
                                .subject(subject)
                                .message(message)
                                .status(NotificationStatus.PENDING)
                                .build();

                        notificationRepository.save(notification);
                        eventPublisher.publishEvent(new NotificationEvent(this, notification, customer, loan));

                        tracker.setStatus(NotificationStatus.SENT);
                        tracker.setSentAt(LocalDateTime.now());
                        trackerRepository.save(tracker);
                    } catch (Exception e) {
                        log.error("Failed to send notification event={} to={}", event, recipient, e);
                        tracker.setStatus(NotificationStatus.FAILED);
                        tracker.setErrorMessage(e.getMessage());
                        trackerRepository.save(tracker);
                    }
                },
                () -> log.warn("No template found for event={} channel={}", event, channel)
        );
    }

    @Override
    @Transactional
    public void sendDueDateReminders(int daysAhead) {
        int total = 0;
        int count;
        while ((count = sendDueDateReminderBatch(daysAhead, 100)) > 0) {
            total += count;
        }
        log.info("Due date reminders complete — processed {} loans", total);
    }

    @Override
    @Transactional
    public int sendDueDateReminderBatch(int daysAhead, int batchSize) {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(daysAhead);
        List<Long> ids = loanRepository.claimLoansDueBetween(from, to, batchSize);
        if (ids.isEmpty()) return 0;

        log.info("Claimed {} loans for due date reminders", ids.size());
        for (Long id : ids) {
            loanRepository.findById(id).ifPresent(loan ->
                    sendNotification(loan.getCustomer(), loan, NotificationEventType.DUE_DATE_REMINDER)
            );
        }
        return ids.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> getAllTemplates() {
        return templateRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NotificationTemplateResponse createTemplate(NotificationTemplateRequest request) {
        NotificationTemplate template = NotificationTemplate.builder()
                .event(request.getEvent())
                .channel(request.getChannel())
                .subject(request.getSubject())
                .body(request.getBody())
                .active(true)
                .build();
        return mapToResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public NotificationTemplateResponse updateTemplate(Long id, NotificationTemplateRequest request) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", id));
        template.setSubject(request.getSubject());
        template.setBody(request.getBody());
        return mapToResponse(templateRepository.save(template));
    }

    // ---- private helpers ----

    private String resolveTemplate(String template, Customer customer, Loan loan) {
        Map<String, String> vars = Map.of(
                "{{customerName}}", customer.getFullName(),
                "{{loanReference}}", loan != null ? loan.getLoanReference() : "",
                "{{amount}}", loan != null ? loan.getPrincipalAmount().toPlainString() : "",
                "{{outstandingBalance}}", loan != null ? loan.getOutstandingBalance().toPlainString() : "",
                "{{dueDate}}", loan != null && loan.getDueDate() != null ? loan.getDueDate().toString() : ""
        );
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private NotificationTemplateResponse mapToResponse(NotificationTemplate t) {
        return NotificationTemplateResponse.builder()
                .id(t.getId())
                .event(t.getEvent())
                .channel(t.getChannel())
                .subject(t.getSubject())
                .body(t.getBody())
                .active(t.getActive())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
