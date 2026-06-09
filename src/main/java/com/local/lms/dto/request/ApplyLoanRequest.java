package com.local.lms.dto.request;

import com.local.lms.domain.enums.BillingCycleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ApplyLoanRequest {
    private Long customerId;
    @NotNull
    @DecimalMin("1.0")
    private BigDecimal amount;
    private BillingCycleType billingCycleType = BillingCycleType.INDIVIDUAL;
    private LocalDate dueDate;
    private String notes;
}
