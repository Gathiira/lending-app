package com.local.lms.scoring;

public interface CrbProvider {
    String name();
    CrbReport fetchReport(String nationalId);
    boolean isAvailable();

    record CrbReport(
            String nationalId,
            int score,
            String summary,
            boolean hasDefaults,
            boolean hasActiveLoans
    ) {}
}
