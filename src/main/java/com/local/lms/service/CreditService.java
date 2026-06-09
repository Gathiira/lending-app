package com.local.lms.service;

import com.local.lms.dto.request.ApproveCustomerLimitRequest;
import com.local.lms.dto.request.CustomerLimitRequest;
import com.local.lms.dto.response.CreditLimitRequestResponse;
import com.local.lms.dto.response.CreditLimitResponse;

import java.util.List;

public interface CreditService {
    List<CreditLimitRequestResponse> getLimitRequests();
    List<CreditLimitRequestResponse> getLimitRequests(Long  customerId);
    List<CreditLimitResponse> getCreditLimit();
    CreditLimitResponse getCustomerCreditLimit(Long customerId);
    CreditLimitRequestResponse applyLimit(Long id, CustomerLimitRequest request);
    CreditLimitRequestResponse updateCreditLimit(Long id, ApproveCustomerLimitRequest request);
}
