package com.local.lms.controller.customer;

import com.local.lms.controller.BaseController;
import com.local.lms.dto.request.ApplyLoanRequest;
import com.local.lms.dto.request.CreateCustomerRequest;
import com.local.lms.dto.request.CustomerLimitRequest;
import com.local.lms.dto.request.RepaymentRequest;
import com.local.lms.dto.response.*;
import com.local.lms.service.CreditService;
import com.local.lms.service.CustomerService;
import com.local.lms.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/credit-limit")
    @Operation(summary = "get credit limit")
    public ResponseResult<CreditLimitResponse> creditLimit() {
        return ResponseResult.success(creditService.getCustomerCreditLimit(getCustomerId()));
    }

    @GetMapping("/limit-request")
    @Operation(summary = "get credit limit request")
    public ResponseResult<List<CreditLimitRequestResponse>> limitRequest() {
        return ResponseResult.success(creditService.getLimitRequests(getCustomerId()));
    }

    @PostMapping("/apply-loan")
    @Operation(summary = "Create and disburse a new loan")
    public ResponseResult<LoanResponse> create(@Validated @RequestBody ApplyLoanRequest request) {
        request.setCustomerId(getCustomerId());
        return ResponseResult.success("Loan created and disbursed", loanService.applyLoan(request));
    }

    @GetMapping("/loans")
    @Operation(summary = "Get all loans for a customer")
    public ResponseResult<List<LoanResponse>> getCustomerLoans() {
        return ResponseResult.success(loanService.getCustomerLoans(getCustomerId()));
    }

    @GetMapping("/loans/{id}")
    @Operation(summary = "Get loan for a customer using loan id")
    public ResponseResult<LoanResponse> getCustomerLoans(@PathVariable Long id) {
        return ResponseResult.success(loanService.getLoan(id, getCustomerId()));
    }

    @PostMapping("/repayments")
    @Operation(summary = "Make a repayment on a loan")
    public ResponseResult<RepaymentResponse> makeRepayment(
            @Validated @RequestBody RepaymentRequest request) {
        request.setCustomerId(getCustomerId());
        return ResponseResult.success("Repayment recorded successfully", loanService.makeRepayment(request));
    }

}
