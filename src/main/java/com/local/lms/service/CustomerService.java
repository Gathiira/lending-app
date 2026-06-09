package com.local.lms.service;

import com.local.lms.dto.request.CreateCustomerRequest;
import com.local.lms.dto.request.UpdateCustomerLimitRequest;
import com.local.lms.dto.response.CustomerResponse;

import java.util.List;

public interface CustomerService {
    CustomerResponse createCustomer(CreateCustomerRequest request);
    CustomerResponse getCustomer(Long id);
    List<CustomerResponse> getAllCustomers();
    CustomerResponse updateCustomer(Long id, CreateCustomerRequest request);
    CustomerResponse updateLoanLimit(Long id, UpdateCustomerLimitRequest request);
    void deactivateCustomer(Long id);
}
