package com.local.lms.dto.request;

import com.local.lms.domain.enums.NotificationChannel;
import com.local.lms.domain.enums.NotificationEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationTemplateRequest {
    @NotNull
    private NotificationEventType event;
    @NotNull
    private NotificationChannel channel;
    private String subject;
    @NotBlank
    private String body;
}
