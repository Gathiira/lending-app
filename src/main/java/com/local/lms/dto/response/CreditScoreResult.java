package com.local.lms.dto.response;

import com.local.lms.domain.enums.RiskBand;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Getter
public class CreditScoreResult {
    private final int totalScore;
    private final RiskBand riskBand;
    private final int loanHistoryScore;
    private final int repaymentScore;
    private final int utilizationScore;
    private final BigDecimal creditLimit;
    private final int crbScore;
    private final String crbSummary;
    private final LocalDateTime evaluatedAt;
}
