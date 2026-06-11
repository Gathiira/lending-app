package com.local.lms.repository;

import com.local.lms.domain.entity.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepaymentRepository extends JpaRepository<Repayment, Long> {

    @Query("SELECT r FROM Repayment r WHERE r.loan.customer.id = :customerId ORDER BY r.paymentDate DESC")
    List<Repayment> findByCustomerId(Long customerId);

    @Query("SELECT r FROM Repayment r WHERE r.loan.id = :loanId AND r.loan.customer.id = :customerId ORDER BY r.paymentDate DESC")
    List<Repayment> findByLoanIdAndCustomerId(Long loanId, Long customerId);
}
