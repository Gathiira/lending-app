package com.local.lms.service.impl;

import com.local.lms.domain.entity.CreditLimit;
import com.local.lms.domain.entity.LoanProduct;
import com.local.lms.domain.enums.RiskBand;
import com.local.lms.dto.response.CreditScoreResult;
import com.local.lms.exceptions.ResourceNotFoundException;
import com.local.lms.repository.CreditLimitRepository;
import com.local.lms.repository.LoanProductRepository;
import com.local.lms.scoring.impl.CrbProfileEvaluator;
import com.local.lms.scoring.impl.CreditUtilizationEvaluator;
import com.local.lms.scoring.impl.LoanHistoryEvaluator;
import com.local.lms.scoring.impl.RepaymentHistoryEvaluator;
import com.local.lms.service.CreditWorthinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditWorthinessServiceImpl implements CreditWorthinessService {

    private final LoanHistoryEvaluator loanHistoryEvaluator;
    private final RepaymentHistoryEvaluator repaymentHistoryEvaluator;
    private final CreditUtilizationEvaluator creditUtilizationEvaluator;
    private final CrbProfileEvaluator crbProfileEvaluator;
    private final CreditLimitRepository creditLimitRepository;
    private final LoanProductRepository loanProductRepository;

    @Override
    public CreditScoreResult evaluate(Long customerId,  Long productId) {
        CreditLimit creditLimit = creditLimitRepository.findByCustomerId(customerId).orElse(null);
        LoanProduct product = null;
        if (creditLimit != null) {
            product  = creditLimit.getLoanProduct();
        } else if (productId != null) {
            product = loanProductRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("LoanProduct", productId));
        }
        return buildResult(customerId, product);
    }

    private CreditScoreResult buildResult(Long customerId, LoanProduct product) {
        int loanScore        = loanHistoryEvaluator.evaluate(customerId);
        int repaymentScore   = repaymentHistoryEvaluator.evaluate(customerId);
        int utilizationScore = creditUtilizationEvaluator.evaluate(customerId);
        int crbScore         = crbProfileEvaluator.evaluate(customerId);

        int total = loanScore + repaymentScore + utilizationScore + crbScore;
        RiskBand riskBand = deriveRiskBand(total);

        // utilization factor — 1.0 for first timers
        double utilizationFactor = creditLimitRepository.findByCustomerId(customerId)
                .map(this::deriveUtilizationFactor)
                .orElse(1.0);

        BigDecimal suggestedLimit = product != null
                ? computeSuggestedLimit(product, total, utilizationFactor)
                : null; // no product passed — score only

        CreditScoreResult result = CreditScoreResult.builder()
                .totalScore(total)
                .riskBand(riskBand)
                .loanHistoryScore(loanScore)
                .repaymentScore(repaymentScore)
                .utilizationScore(utilizationScore)
                .crbScore(crbScore)
                .crbSummary(crbProfileEvaluator.crbSummary())
                .creditLimit(suggestedLimit)
                .evaluatedAt(LocalDateTime.now())
                .build();

        log.info("Credit score evaluated [customerId={}, total={}, band={}, suggestedLimit={}]",
                customerId, total, riskBand, suggestedLimit);
        return result;
    }

    private BigDecimal computeSuggestedLimit(LoanProduct product, int score, double utilizationFactor) {
        BigDecimal minAmount = product.getMinAmount();
        BigDecimal maxAmount = product.getMaxAmount();

        double scoreMultiplier = deriveScoreMultiplier(score);

        BigDecimal suggested = maxAmount
                .multiply(BigDecimal.valueOf(scoreMultiplier))
                .multiply(BigDecimal.valueOf(utilizationFactor))
                .setScale(2, RoundingMode.HALF_UP);

        if (suggested.compareTo(minAmount) < 0) return minAmount;
        if (suggested.compareTo(maxAmount) > 0) return maxAmount;
        return suggested;
    }

    private double deriveScoreMultiplier(int score) {
        if (score >= 800) return 1.00;
        if (score >= 600) return 0.75;
        if (score >= 400) return 0.50;
        if (score >= 200) return 0.25;
        return 0.10;
    }

    private double deriveUtilizationFactor(CreditLimit creditLimit) {
        if (creditLimit.getCurrentLimit().compareTo(BigDecimal.ZERO) == 0) return 1.0;

        BigDecimal used = creditLimit.getCurrentLimit().subtract(creditLimit.getAvailableLimit());
        double utilization = used.divide(creditLimit.getCurrentLimit(), 4, RoundingMode.HALF_UP)
                .doubleValue();

        if (utilization <= 0.30) return 1.00;
        if (utilization <= 0.50) return 0.90;
        if (utilization <= 0.70) return 0.75;
        return 0.60;
    }

    private RiskBand deriveRiskBand(int score) {
        if (score >= 800) return RiskBand.EXCELLENT;
        if (score >= 600) return RiskBand.GOOD;
        if (score >= 400) return RiskBand.FAIR;
        if (score >= 200) return RiskBand.POOR;
        return RiskBand.VERY_POOR;
    }
}