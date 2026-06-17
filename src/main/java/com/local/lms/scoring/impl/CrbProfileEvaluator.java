package com.local.lms.scoring.impl;

import com.local.lms.domain.entity.Customer;
import com.local.lms.repository.CustomerRepository;
import com.local.lms.scoring.CrbProvider;
import com.local.lms.scoring.CreditScoreEvaluator;
import com.local.lms.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CrbProfileEvaluator implements CreditScoreEvaluator {

    private final CustomerRepository customerRepository;
    private final List<CrbProvider> crbProviders;

    @Override public String name() { return "CRB_PROFILE"; }
    @Override public int maxScore() { return 100; }

    @Override
    public int evaluate(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

        CrbProvider provider = crbProviders.stream()
                .filter(CrbProvider::isAvailable)
                .findFirst()
                .orElse(null);

        if (provider == null) {
            log.info("CRB evaluation skipped — no provider available [customerId={}]", customerId);
            return 50;
        }

        try {
            CrbProvider.CrbReport report = provider.fetchReport(customer.getNationalId());
            log.info("CRB evaluation from {} [customerId={}, score={}]",
                    provider.name(), customerId, report.score());
            return normalizeScore(report.score());
        } catch (Exception e) {
            log.error("CRB provider {} failed for customerId={}", provider.name(), customerId, e);
            return 50;
        }
    }

    public String crbSummary() {
        return crbProviders.stream()
                .filter(CrbProvider::isAvailable)
                .findFirst()
                .map(CrbProvider::name)
                .orElse("No CRB provider available");
    }

    private int normalizeScore(int rawScore) {
        return Math.max(0, Math.min(100, rawScore));
    }
}
