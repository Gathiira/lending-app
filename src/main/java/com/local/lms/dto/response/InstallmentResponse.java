package com.local.lms.dto.response;

import com.local.lms.domain.enums.LoanStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class InstallmentResponse {
    private Long id;
    private Integer installmentNumber;
    private BigDecimal principalAmount;
    private BigDecimal outstandingAmount;
    private LocalDate dueDate;
    private LocalDate paidDate;
    private LoanStatus status;
}
