package com.local.lms.service;

import com.local.lms.dto.request.ApplyLoanRequest;
import com.local.lms.dto.request.CreateLoanRequest;
import com.local.lms.dto.request.LoanSearchRequest;
import com.local.lms.dto.request.RepaymentRequest;
import com.local.lms.dto.response.LoanResponse;
import com.local.lms.dto.response.PaginatedResponse;
import com.local.lms.dto.response.RepaymentResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LoanService {
    LoanResponse createLoan(CreateLoanRequest request);
    LoanResponse applyLoan(ApplyLoanRequest request);
    LoanResponse getLoan(Long id);
    LoanResponse getLoan(Long id, Long customerId);
    LoanResponse getLoanByReference(String reference);
    List<LoanResponse> getCustomerLoans(Long customerId);
    List<LoanResponse> getLoans();
    PaginatedResponse<LoanResponse> getPage(LoanSearchRequest request, Pageable pageable);
    RepaymentResponse makeRepayment(RepaymentRequest request);
    List<RepaymentResponse> getLoanRepayments(Long id, Long customerId);
    LoanResponse cancelLoan(Long id);
    LoanResponse writeOffLoan(Long id);

    // Called by scheduler — batch variants use FOR UPDATE SKIP LOCKED
    void processOverdueLoans();
    int processOverdueBatch(int batchSize);
    void applyDailyFees();
    int applyDailyFeesBatch(int batchSize);
}
