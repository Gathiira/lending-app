package com.local.lms.dto.response;

import com.local.lms.domain.enums.FeeCalculationMethod;
import com.local.lms.domain.enums.FeeType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductFeeResponse {
    private Long id;
    private FeeType feeType;
    private FeeCalculationMethod calculationMethod;
    private BigDecimal amount;
    private Integer daysAfterDue;
    private String description;
}
