package com.local.lms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateCustomerLimitRequest {
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal maxLoanLimit;
    private String reason;
}
