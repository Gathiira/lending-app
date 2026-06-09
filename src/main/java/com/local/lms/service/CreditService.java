package com.local.lms.service;

import com.local.lms.dto.request.ApproveCustomerLimitRequest;
import com.local.lms.dto.request.CustomerLimitRequest;
import com.local.lms.dto.response.CreditLimitRequestResponse;
import com.local.lms.dto.response.CreditLimitResponse;

import java.util.List;

public interface CreditService {
    List<CreditLimitRequestResponse> getLimitRequests();
    List<CreditLimitResponse> getCreditLimit();
    CreditLimitRequestResponse applyLimit(Long id, CustomerLimitRequest request);
    CreditLimitRequestResponse updateCreditLimit(Long id, ApproveCustomerLimitRequest request);
}
