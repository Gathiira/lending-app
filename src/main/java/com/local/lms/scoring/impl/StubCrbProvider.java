package com.local.lms.scoring.impl;

import com.local.lms.scoring.CrbProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(CrbProvider.class)
@Slf4j
public class StubCrbProvider implements CrbProvider {

    @Override
    public String name() {
        return "STUB";
    }

    @Override
    public CrbReport fetchReport(String nationalId) {
        log.info("Stub CRB report for nationalId={} — returning neutral score", nationalId);
        return new CrbReport(nationalId, 50, "Stub report — no real CRB integration", false, false);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
