package com.local.lms.dto.response;

import com.local.lms.domain.enums.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class LoanResponse {
    private Long id;
    private String loanReference;
    private Long customerId;
    private String customerName;
    private Long productId;
    private String productName;
    private BigDecimal principalAmount;
    private BigDecimal outstandingBalance;
    private LoanType loanType;
    private LoanStatus loanStatus;
    private BillingCycleType billingCycleType;
    private LocalDate dueDate;
    private LocalDate disbursementDate;
    private LocalDate closedDate;
    private String notes;
    private List<InstallmentResponse> installments;
    private List<LoanFeeResponse> fees;
    private LocalDateTime createdAt;
}
