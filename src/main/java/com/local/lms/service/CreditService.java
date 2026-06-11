package com.local.lms.service;

import com.local.lms.dto.request.ApproveCustomerLimitRequest;
import com.local.lms.dto.request.CreditLimitAdjustmentRequest;
import com.local.lms.dto.request.CreditSearchRequest;
import com.local.lms.dto.request.CustomerLimitRequest;
import com.local.lms.dto.response.CreditLimitRequestResponse;
import com.local.lms.dto.response.CreditLimitResponse;
import com.local.lms.dto.response.PaginatedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CreditService {
    List<CreditLimitRequestResponse> getLimitRequests();
    PaginatedResponse<CreditLimitRequestResponse> getPage(CreditSearchRequest request, Pageable pageable);
    List<CreditLimitRequestResponse> getLimitRequests(Long  customerId);
    List<CreditLimitResponse> getCreditLimit();
    CreditLimitResponse getCustomerCreditLimit(Long customerId);
    CreditLimitRequestResponse applyLimit(Long id, CustomerLimitRequest request);
    CreditLimitRequestResponse updateCreditLimit(Long id, ApproveCustomerLimitRequest request);
    CreditLimitResponse adjustCreditLimit(Long customerId, CreditLimitAdjustmentRequest request);
}
