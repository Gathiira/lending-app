package com.local.lms.scoring;

public interface CreditScoreEvaluator {
    String name();
    int maxScore();
    int evaluate(Long customerId);
}
