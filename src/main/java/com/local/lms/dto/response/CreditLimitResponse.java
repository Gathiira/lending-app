package com.local.lms.dto.response;

import com.local.lms.domain.enums.CreditLimitStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CreditLimitResponse {
    private Long id;
    private BigDecimal currentLimit;
    private BigDecimal frozenLimit;
    private BigDecimal availableLimit;
    private CreditLimitStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ProductResponse loanProduct;
}
