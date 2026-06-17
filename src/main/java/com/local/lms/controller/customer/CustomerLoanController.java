package com.local.lms.controller.customer;

import com.local.lms.controller.BaseController;
import com.local.lms.dto.request.ApplyLoanRequest;
import com.local.lms.dto.request.RepaymentRequest;
import com.local.lms.dto.response.LoanResponse;
import com.local.lms.dto.response.RepaymentResponse;
import com.local.lms.dto.response.ResponseResult;
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
@Tag(name = "Customer Loans", description = "Customer-facing loan APIs")
public class CustomerLoanController extends BaseController {

    private final LoanService loanService;

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
    public ResponseResult<LoanResponse> getCustomerLoan(@PathVariable Long id) {
        return ResponseResult.success(loanService.getLoan(id, getCustomerId()));
    }

    @GetMapping("/loans/{id}/repayments")
    @Operation(summary = "Get loan repayments for a customer using loan id")
    public ResponseResult<List<RepaymentResponse>> getCustomerLoanRepayments(@PathVariable Long id) {
        return ResponseResult.success(loanService.getLoanRepayments(id, getCustomerId()));
    }

    @PostMapping("/repayments")
    @Operation(summary = "Make a repayment on a loan")
    public ResponseResult<RepaymentResponse> makeRepayment(
            @Validated @RequestBody RepaymentRequest request) {
        request.setCustomerId(getCustomerId());
        return ResponseResult.success("Repayment recorded successfully", loanService.makeRepayment(request));
    }

}
