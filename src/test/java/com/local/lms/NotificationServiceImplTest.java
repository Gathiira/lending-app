package com.local.lms;

import com.local.lms.domain.entity.*;
import com.local.lms.domain.enums.*;
import com.local.lms.dto.request.NotificationTemplateRequest;
import com.local.lms.dto.response.NotificationTemplateResponse;
import com.local.lms.repository.LoanRepository;
import com.local.lms.repository.NotificationRepository;
import com.local.lms.repository.NotificationTrackerRepository;
import com.local.lms.repository.NotificationTemplateRepository;
import com.local.lms.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Mock private NotificationTemplateRepository templateRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationTrackerRepository trackerRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    // -----------------------------
    // SEND NOTIFICATION
    // -----------------------------
    @Test
    void sendNotification_shouldSaveNotification() {

        Customer customer = Customer.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@mail.com")
                .phoneNumber("0712345678")
                .preferredChannel(NotificationChannel.EMAIL)
                .build();

        Loan loan = Loan.builder()
                .id(1L)
                .loanReference("LN-001")
                .principalAmount(new BigDecimal("1000"))
                .outstandingBalance(new BigDecimal("500"))
                .dueDate(LocalDate.now().plusDays(5))
                .build();

        NotificationTemplate template = NotificationTemplate.builder()
                .subject("Hello {{customerName}}")
                .body("Loan {{loanReference}} amount {{amount}}")
                .active(true)
                .build();

        when(trackerRepository.existsByLoanIdAndEventAndNotificationDate(any(), any(), any()))
                .thenReturn(false);
        when(trackerRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(templateRepository.findByEventAndChannelAndActiveTrue(any(), any()))
                .thenReturn(Optional.of(template));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.sendNotification(customer, loan, NotificationEventType.LOAN_CREATED);

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(trackerRepository, times(3)).save(any(NotificationTracker.class));
        verify(eventPublisher).publishEvent(any());
    }

    // -----------------------------
    // TEMPLATE NOT FOUND
    // -----------------------------
    @Test
    void sendNotification_shouldDoNothing_whenTemplateMissing() {

        Customer customer = Customer.builder()
                .id(1L)
                .preferredChannel(NotificationChannel.EMAIL)
                .build();

        Loan loan = Loan.builder()
                .id(1L)
                .build();

        when(trackerRepository.existsByLoanIdAndEventAndNotificationDate(any(), any(), any()))
                .thenReturn(false);
        when(templateRepository.findByEventAndChannelAndActiveTrue(any(), any()))
                .thenReturn(Optional.empty());

        notificationService.sendNotification(customer, loan, NotificationEventType.LOAN_CREATED);

        verify(notificationRepository, never()).save(any());
    }

    // -----------------------------
    // IDEMPOTENCY — SKIP IF ALREADY TRACKED
    // -----------------------------
    @Test
    void sendNotification_shouldSkip_whenAlreadyTracked() {

        Customer customer = Customer.builder()
                .id(1L)
                .preferredChannel(NotificationChannel.EMAIL)
                .build();

        Loan loan = Loan.builder()
                .id(1L)
                .build();

        when(trackerRepository.existsByLoanIdAndEventAndNotificationDate(any(), any(), any()))
                .thenReturn(true);

        notificationService.sendNotification(customer, loan, NotificationEventType.LOAN_CREATED);

        verify(notificationRepository, never()).save(any());
        verify(templateRepository, never()).findByEventAndChannelAndActiveTrue(any(), any());
    }

    // -----------------------------
    // EMAIL SEND PATH
    // -----------------------------
    @Test
    void sendNotification_shouldSendEmail_whenEmailEnabled() {

        Customer customer = Customer.builder()
                .id(1L)
                .email("john@gmail.com")
                .phoneNumber("0712345678")
                .preferredChannel(NotificationChannel.EMAIL)
                .build();

        Loan loan = Loan.builder()
                .id(1L)
                .loanReference("LN-001")
                .principalAmount(new BigDecimal("1000"))
                .outstandingBalance(new BigDecimal("500"))
                .build();

        NotificationTemplate template = NotificationTemplate.builder()
                .subject("Subject {{customerName}}")
                .body("Body {{loanReference}}")
                .active(true)
                .build();

        when(trackerRepository.existsByLoanIdAndEventAndNotificationDate(any(), any(), any()))
                .thenReturn(false);
        when(trackerRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(templateRepository.findByEventAndChannelAndActiveTrue(any(), any()))
                .thenReturn(Optional.of(template));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.sendNotification(customer, loan, NotificationEventType.LOAN_CREATED);

        verify(notificationRepository).save(any(Notification.class));
    }

    // -----------------------------
    // DUE DATE REMINDERS
    // -----------------------------
    @Test
    void sendDueDateReminders_shouldClaimAndNotify() {

        Customer customer = Customer.builder()
                .id(1L)
                .preferredChannel(NotificationChannel.EMAIL)
                .build();

        Loan loan = Loan.builder()
                .id(1L)
                .customer(customer)
                .build();

        when(loanRepository.claimLoansDueBetween(any(), any(), anyInt()))
                .thenReturn(List.of(1L))
                .thenReturn(List.of());
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(trackerRepository.existsByLoanIdAndEventAndNotificationDate(any(), any(), any()))
                .thenReturn(false);
        when(templateRepository.findByEventAndChannelAndActiveTrue(any(), any()))
                .thenReturn(Optional.empty());

        notificationService.sendDueDateReminders(5);

        verify(loanRepository, times(2)).claimLoansDueBetween(any(), any(), anyInt());
    }

    // -----------------------------
    // CREATE TEMPLATE
    // -----------------------------
    @Test
    void createTemplate_success() {

        NotificationTemplateRequest request = new NotificationTemplateRequest();
        request.setEvent(NotificationEventType.LOAN_CREATED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setSubject("Hello");
        request.setBody("Body");

        NotificationTemplate saved = NotificationTemplate.builder()
                .id(1L)
                .event(request.getEvent())
                .channel(request.getChannel())
                .subject("Hello")
                .body("Body")
                .active(true)
                .build();

        when(templateRepository.save(any()))
                .thenReturn(saved);

        NotificationTemplateResponse response = notificationService.createTemplate(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);

        verify(templateRepository).save(any(NotificationTemplate.class));
    }

    // -----------------------------
    // UPDATE TEMPLATE
    // -----------------------------
    @Test
    void updateTemplate_success() {

        NotificationTemplate existing = NotificationTemplate.builder()
                .id(1L)
                .subject("Old")
                .body("Old body")
                .active(true)
                .build();

        NotificationTemplateRequest request = new NotificationTemplateRequest();
        request.setSubject("New");
        request.setBody("New body");

        when(templateRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(templateRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        NotificationTemplateResponse response = notificationService.updateTemplate(1L, request);

        assertThat(response.getSubject()).isEqualTo("New");

        verify(templateRepository).save(existing);
    }

    // -----------------------------
    // UPDATE TEMPLATE - NOT FOUND
    // -----------------------------
    @Test
    void updateTemplate_shouldThrow_whenNotFound() {

        when(templateRepository.findById(1L))
                .thenReturn(Optional.empty());

        NotificationTemplateRequest request = new NotificationTemplateRequest();
        request.setSubject("New");

        assertThatThrownBy(() -> notificationService.updateTemplate(1L, request))
                .isInstanceOf(RuntimeException.class);
    }

    // -----------------------------
    // GET ALL TEMPLATES
    // -----------------------------
    @Test
    void getAllTemplates_success() {

        NotificationTemplate template = NotificationTemplate.builder()
                .id(1L)
                .event(NotificationEventType.LOAN_CREATED)
                .channel(NotificationChannel.EMAIL)
                .subject("Sub")
                .body("Body")
                .active(true)
                .build();

        when(templateRepository.findAll())
                .thenReturn(List.of(template));

        List<NotificationTemplateResponse> responses = notificationService.getAllTemplates();

        assertThat(responses).hasSize(1);
    }
}
