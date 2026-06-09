package com.local.lms.dto.response;

import com.local.lms.domain.enums.ApprovalStatus;
import com.local.lms.domain.enums.NotificationChannel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CreditLimitRequestResponse {
    private Long id;
    private String fileUrl;
    private BigDecimal approvedLimit;
    private String reason;
    private ApprovalStatus status;
    private String reviewNotes;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
