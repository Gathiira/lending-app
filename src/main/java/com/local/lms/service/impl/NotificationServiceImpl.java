package com.local.lms.service.impl;

import com.local.lms.domain.entity.*;
import com.local.lms.domain.enums.NotificationChannel;
import com.local.lms.domain.enums.NotificationEventType;
import com.local.lms.domain.enums.NotificationStatus;
import com.local.lms.dto.request.NotificationTemplateRequest;
import com.local.lms.dto.response.NotificationTemplateResponse;
import com.local.lms.exceptions.ResourceNotFoundException;
import com.local.lms.repository.LoanRepository;
import com.local.lms.repository.NotificationRepository;
import com.local.lms.repository.NotificationTemplateRepository;
import com.local.lms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final LoanRepository loanRepository;

    @Override
    @Transactional
    public void sendNotification(Customer customer, Loan loan, NotificationEventType event) {
        NotificationChannel channel = customer.getPreferredChannel();

        templateRepository.findByEventAndChannelAndActiveTrue(event, channel).ifPresentOrElse(
                template -> {
                    String message = resolveTemplate(template.getBody(), customer, loan);
                    String subject = template.getSubject() != null
                            ? resolveTemplate(template.getSubject(), customer, loan) : null;
                    String recipient = channel == NotificationChannel.SMS
                            ? customer.getPhoneNumber() : customer.getEmail();

                    Notification notifLog = Notification.builder()
                            .customer(customer)
                            .loan(loan)
                            .event(event)
                            .channel(channel)
                            .recipient(recipient)
                            .subject(subject)
                            .message(message)
                            .status(NotificationStatus.PENDING)
                            .build();

                    try {
                        // switch different channels and using the respective impl
                        log.info("[NOTIFICATION] Channel {} not configured. Would send to {} the message: {}", channel, recipient, message);
                        notifLog.setStatus(NotificationStatus.SENT);
                        notifLog.setSentAt(LocalDateTime.now());
                    } catch (Exception e) {
                        log.error("Failed to send notification event={} to={}", event, recipient, e);
                        notifLog.setStatus(NotificationStatus.FAILED);
                        notifLog.setErrorMessage(e.getMessage());
                    }
                    notificationRepository.save(notifLog);
                },
                () -> log.warn("No template found for event={} channel={}", event, channel)
        );
    }

    @Override
    @Transactional
    public void sendDueDateReminders(int daysAhead) {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(daysAhead);
        List<Loan> dueLoans = loanRepository.findLoansDueBetween(from, to);
        log.info("Sending due date reminders for {} loans due in next {} days", dueLoans.size(), daysAhead);
        dueLoans.forEach(loan -> sendNotification(loan.getCustomer(), loan, NotificationEventType.DUE_DATE_REMINDER));
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
