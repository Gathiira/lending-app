package com.local.lms.scoring.impl;

import com.local.lms.domain.entity.Repayment;
import com.local.lms.repository.RepaymentRepository;
import com.local.lms.scoring.CreditScoreEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
@RequiredArgsConstructor
@Slf4j
public class RepaymentHistoryEvaluator implements CreditScoreEvaluator {

    private final RepaymentRepository repaymentRepository;

    @Override public String name() { return "REPAYMENT_HISTORY"; }
    @Override public int maxScore() { return 400; }

    @Override
    public int evaluate(Long customerId) {
        List<Repayment> repayments = repaymentRepository.findByCustomerId(customerId);

        if (repayments.isEmpty()) return 200; // no history — neutral

        long total   = repayments.size();
        long onTime  = repayments.stream()
                .filter(r -> !r.getPaymentDate().isAfter(r.getLoan().getDueDate()))
                .count();

        double onTimeRate = (double) onTime / total;

        if (onTimeRate >= 0.95) return 400;
        if (onTimeRate >= 0.85) return 320;
        if (onTimeRate >= 0.70) return 220;
        if (onTimeRate >= 0.50) return 120;
        return 40;
    }
}
