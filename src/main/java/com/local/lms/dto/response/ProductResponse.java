package com.local.lms.dto.response;

import com.local.lms.domain.enums.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal interestRate;
    private Integer tenureValue;
    private TenureType tenureType;
    private LoanType loanType;
    private Integer installmentCount;
    private BillingCycleType billingCycleType;
    private Integer gracePeriodDays;
    private Boolean active;
    private List<ProductFeeResponse> fees;
    private LocalDateTime createdAt;
}