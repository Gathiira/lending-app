package com.local.lms.dto.response;

import com.local.lms.domain.enums.FeeType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class LoanFeeResponse {
    private Long id;
    private FeeType feeType;
    private BigDecimal amount;
    private LocalDate appliedDate;
    private Boolean paid;
    private LocalDate paidDate;
    private String description;
}
