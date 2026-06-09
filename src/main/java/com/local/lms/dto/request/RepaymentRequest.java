package com.local.lms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RepaymentRequest {
    @NotNull
    private Long loanId;
    @NotNull
    @DecimalMin("1.0")
    private BigDecimal amount;
    private Long installmentId;
    private LocalDate paymentDate;
    private String notes;
}
