package com.local.lms.service;

import com.local.lms.dto.response.CreditScoreResult;

public interface CreditWorthinessService {
    CreditScoreResult evaluate(Long customerId, Long productId);
}
