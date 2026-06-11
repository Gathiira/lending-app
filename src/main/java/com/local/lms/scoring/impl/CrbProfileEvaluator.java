package com.local.lms.scoring.impl;

import com.local.lms.scoring.CreditScoreEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CrbProfileEvaluator implements CreditScoreEvaluator {

    @Override public String name() { return "CRB_PROFILE"; }
    @Override public int maxScore() { return 100; }

    @Override
    public int evaluate(Long customerId) {
        // TODO: integrate with CRB provider (e.g. Metropol, TransUnion KE)
        log.info("CRB evaluation skipped — integration pending [customerId={}]", customerId);
        return 50; // neutral stub score until integrated
    }

    public String crbSummary() {
        return "CRB integration pending";
    }
}
