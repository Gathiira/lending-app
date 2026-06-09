package com.local.lms.repository;

import com.local.lms.domain.entity.Loan;
import com.local.lms.domain.entity.LoanInstallment;
import com.local.lms.domain.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanInstallmentRepository  extends JpaRepository<LoanInstallment, Long> {
    List<LoanInstallment> findByLoanAndStatusOrderByDueDateAsc(
            Loan loan,
            LoanStatus status
    );
}
