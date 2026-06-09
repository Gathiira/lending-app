package com.local.lms.dto.response;

import com.local.lms.domain.enums.NotificationChannel;
import com.local.lms.domain.enums.NotificationEventType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationTemplateResponse {
    private Long id;
    private NotificationEventType event;
    private NotificationChannel channel;
    private String subject;
    private String body;
    private Boolean active;
    private LocalDateTime createdAt;
}
