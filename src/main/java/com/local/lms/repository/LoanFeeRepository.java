package com.local.lms.repository;

import com.local.lms.domain.entity.LoanFee;
import com.local.lms.domain.enums.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanFeeRepository extends JpaRepository<LoanFee, Long> {
    List<LoanFee> findByLoanIdAndPaidFalse(Long loanId);
    List<LoanFee> findByLoanIdAndFeeTypeAndPaidFalse(Long loanId, FeeType feeType);
    boolean existsByLoanIdAndFeeTypeAndPaidFalse(Long loanId, FeeType feeType);
}
