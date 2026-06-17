package com.local.lms.service;

import com.local.lms.domain.entity.Customer;
import com.local.lms.domain.entity.Loan;
import com.local.lms.domain.enums.NotificationEventType;
import com.local.lms.dto.request.NotificationTemplateRequest;
import com.local.lms.dto.response.NotificationTemplateResponse;

import java.util.List;

public interface NotificationService {
    void sendNotification(Customer customer, Loan loan, NotificationEventType event);
    List<NotificationTemplateResponse> getAllTemplates();
    NotificationTemplateResponse createTemplate(NotificationTemplateRequest request);
    NotificationTemplateResponse updateTemplate(Long id, NotificationTemplateRequest request);
    void sendDueDateReminders(int daysAhead);
    int sendDueDateReminderBatch(int daysAhead, int batchSize);
}