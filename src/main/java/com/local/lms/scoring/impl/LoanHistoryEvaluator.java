package com.local.lms.scoring.impl;

import com.local.lms.domain.entity.Loan;
import com.local.lms.domain.enums.LoanStatus;
import com.local.lms.repository.LoanRepository;
import com.local.lms.scoring.CreditScoreEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanHistoryEvaluator implements CreditScoreEvaluator {

    private final LoanRepository loanRepository;

    @Override public String name() { return "LOAN_HISTORY"; }
    @Override public int maxScore() { return 300; }

    @Override
    public int evaluate(Long customerId) {
        List<Loan> loans = loanRepository.findByCustomerId(customerId);

        if (loans.isEmpty()) return 150; // no history — neutral mid-score

        long total    = loans.size();
        long defaults = loans.stream()
                .filter(l -> l.getStatus() == LoanStatus.OVERDUE)
                .count();

        double defaultRate = (double) defaults / total;

        // scoring can be adjusted
        if (defaultRate == 0)    return 300;
        if (defaultRate <= 0.05) return 250;
        if (defaultRate <= 0.10) return 180;
        if (defaultRate <= 0.20) return 100;
        return 0;
    }
}
