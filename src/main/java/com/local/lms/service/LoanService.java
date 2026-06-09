package com.local.lms.service;

import com.local.lms.dto.request.CreateLoanRequest;
import com.local.lms.dto.request.RepaymentRequest;
import com.local.lms.dto.response.LoanResponse;
import com.local.lms.dto.response.RepaymentResponse;

import java.util.List;

public interface LoanService {
    LoanResponse createLoan(CreateLoanRequest request);
    LoanResponse getLoan(Long id);
    LoanResponse getLoanByReference(String reference);
    List<LoanResponse> getCustomerLoans(Long customerId);
    List<LoanResponse> getLoans();
    RepaymentResponse makeRepayment(RepaymentRequest request);
    LoanResponse cancelLoan(Long id);
    LoanResponse writeOffLoan(Long id);

    // Called by scheduler
    void processOverdueLoans();
    void applyDailyFees();
}
