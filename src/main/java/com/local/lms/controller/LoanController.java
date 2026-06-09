package com.local.lms.controller;

import com.local.lms.dto.request.CreateLoanRequest;
import com.local.lms.dto.response.ResponseResult;
import com.local.lms.dto.response.LoanResponse;
import com.local.lms.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Loan management APIs")
public class LoanController  extends  BaseController{

    private final LoanService loanService;

    @GetMapping()
    @Operation(summary = "Get all loans")
    public ResponseResult<List<LoanResponse>> getLoans() {
        return ResponseResult.success(loanService.getLoans());
    }

    @PostMapping
    @Operation(summary = "Create and disburse a new loan")
    public ResponseResult<LoanResponse> create(@Validated @RequestBody CreateLoanRequest request) {
        return ResponseResult.success("Loan created and disbursed", loanService.createLoan(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan by ID")
    public ResponseResult<LoanResponse> getById(@PathVariable Long id) {
        return ResponseResult.success(loanService.getLoan(id));
    }

    @GetMapping("/reference/{reference}")
    @Operation(summary = "Get loan by reference number")
    public ResponseResult<LoanResponse> getByReference(@PathVariable String reference) {
        return ResponseResult.success(loanService.getLoanByReference(reference));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all loans for a customer")
    public ResponseResult<List<LoanResponse>> getCustomerLoans(@PathVariable Long customerId) {
        return ResponseResult.success(loanService.getCustomerLoans(customerId));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a loan")
    public ResponseResult<LoanResponse> cancel(@PathVariable Long id) {
        return ResponseResult.success("Loan cancelled", loanService.cancelLoan(id));
    }

    @PatchMapping("/{id}/write-off")
    @Operation(summary = "Write off an overdue loan")
    public ResponseResult<LoanResponse> writeOff(@PathVariable Long id) {
        return ResponseResult.success("Loan written off", loanService.writeOffLoan(id));
    }

    @PostMapping("/sweep/overdue")
    @Operation(summary = "Manually trigger overdue loan sweep (admin)")
    public ResponseResult<Void> triggerOverdueSweep() {
        loanService.processOverdueLoans();
        return ResponseResult.success("Overdue sweep completed");
    }
}