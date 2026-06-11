package com.local.lms.dto.request;

import com.local.lms.domain.enums.AdjustmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditLimitAdjustmentRequest {
    @NotNull
    private AdjustmentType type; // INCREASE or DECREASE

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String reason;
}
