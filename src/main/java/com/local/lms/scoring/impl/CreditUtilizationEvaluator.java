package com.local.lms.scoring.impl;

import com.local.lms.repository.CreditLimitRepository;

import com.local.lms.scoring.CreditScoreEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreditUtilizationEvaluator implements CreditScoreEvaluator {

    private final CreditLimitRepository creditLimitRepository;

    @Override public String name() { return "CREDIT_UTILIZATION"; }
    @Override public int maxScore() { return 200; }

    @Override
    public int evaluate(Long customerId) {
        return creditLimitRepository.findByCustomerId(customerId)
                .map(cl -> {
                    if (cl.getCurrentLimit().compareTo(BigDecimal.ZERO) == 0) return 100;

                    BigDecimal used = cl.getCurrentLimit().subtract(cl.getAvailableLimit());
                    double utilization = used.divide(cl.getCurrentLimit(), 4, RoundingMode.HALF_UP).doubleValue();

                    if (utilization <= 0.30) return 200;
                    if (utilization <= 0.50) return 160;
                    if (utilization <= 0.70) return 100;
                    if (utilization <= 0.90) return 50;
                    return 10;
                })
                .orElse(100); // no limit record — neutral
    }
}
