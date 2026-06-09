package com.local.lms.dto.request;

import com.local.lms.domain.enums.ApprovalStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApproveCustomerLimitRequest {
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal maxLoanLimit;
    @NotNull
    private Long productId;
    @NotBlank
    private String reason;
    @NotNull
    private ApprovalStatus status;
}
