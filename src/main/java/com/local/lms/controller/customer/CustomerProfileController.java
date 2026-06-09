package com.local.lms.controller.customer;

import com.local.lms.controller.BaseController;
import com.local.lms.dto.request.ApplyLoanRequest;
import com.local.lms.dto.request.CreateCustomerRequest;
import com.local.lms.dto.request.CreateLoanRequest;
import com.local.lms.dto.request.CustomerLimitRequest;
import com.local.lms.dto.response.CreditLimitRequestResponse;
import com.local.lms.dto.response.CustomerResponse;
import com.local.lms.dto.response.LoanResponse;
import com.local.lms.dto.response.ResponseResult;
import com.local.lms.service.CreditService;
import com.local.lms.service.CustomerService;
import com.local.lms.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/customer")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "Customer profile management APIs")
public class CustomerProfileController extends BaseController {

    private final CustomerService customerService;
    private final CreditService creditService;
    private final LoanService loanService;


    @GetMapping
    @Operation(summary = "Get customer by ID")
    public ResponseResult<CustomerResponse> getById() {
        return ResponseResult.success(customerService.getCustomer(getCustomerId()));
    }

    @PutMapping
    @Operation(summary = "Update customer information")
    public ResponseResult<CustomerResponse> update(@Validated @RequestBody CreateCustomerRequest request) {
        return ResponseResult.success("Customer updated", customerService.updateCustomer(getCustomerId(), request));
    }

    @PostMapping("/apply-limit")
    @Operation(summary = "apply credit limit")
    public ResponseResult<CreditLimitRequestResponse> applyCreditLimit(@Validated @RequestBody CustomerLimitRequest request) {
        return ResponseResult.success("Credit limit Applied", creditService.applyLimit(getCustomerId(), request));
    }

    @PostMapping("/apply-loan")
    @Operation(summary = "Create and disburse a new loan")
    public ResponseResult<LoanResponse> create(@Validated @RequestBody ApplyLoanRequest request) {
        request.setCustomerId(getCustomerId());
        return ResponseResult.success("Loan created and disbursed", loanService.applyLoan(request));
    }

}
