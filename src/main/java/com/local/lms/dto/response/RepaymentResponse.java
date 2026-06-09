package com.local.lms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class RepaymentResponse {
    private Long id;
    private String repaymentReference;
    private Long loanId;
    private String loanReference;
    private BigDecimal amount;
    private BigDecimal principalPaid;
    private BigDecimal feesPaid;
    private LocalDate paymentDate;
    private String notes;
    private LocalDateTime createdAt;
}
