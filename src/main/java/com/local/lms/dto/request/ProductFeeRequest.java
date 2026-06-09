package com.local.lms.dto.request;

import com.local.lms.domain.enums.FeeCalculationMethod;
import com.local.lms.domain.enums.FeeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductFeeRequest {
    @NotNull
    private FeeType feeType;
    @NotNull
    private FeeCalculationMethod calculationMethod;
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal amount;
    @Min(0)
    private Integer daysAfterDue = 0;
    private String description;
}
