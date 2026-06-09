package com.local.lms.repository;

import com.local.lms.domain.entity.Loan;
import com.local.lms.domain.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByLoanReference(String loanReference);

    Optional<Loan> findByIdAndCustomerId(Long id,  Long customerId);

    List<Loan> findByCustomerId(Long customerId);

    List<Loan> findByCustomerIdAndStatus(Long customerId, LoanStatus status);

    /** Loans that are OPEN but past their due date — candidates for OVERDUE sweep */
    @Query("SELECT l FROM Loan l WHERE l.status = 'OPEN' AND l.dueDate < :today")
    List<Loan> findOpenLoansOverdue(@Param("today") LocalDate today);

    /** Loans already OVERDUE — candidates for WRITTEN_OFF sweep */
    @Query("SELECT l FROM Loan l WHERE l.status = 'OVERDUE' AND l.dueDate < :cutoffDate")
    List<Loan> findOverdueLoansBeforeDate(@Param("cutoffDate") LocalDate cutoffDate);

    /** Loans due in the next N days for reminder notifications */
    @Query("SELECT l FROM Loan l WHERE l.status IN ('OPEN', 'OVERDUE') AND l.dueDate BETWEEN :from AND :to")
    List<Loan> findLoansDueBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Check whether a customer has any active loans */
    @Query("SELECT COUNT(l) > 0 FROM Loan l WHERE l.customer.id = :customerId AND l.status IN ('OPEN', 'OVERDUE')")
    boolean customerHasActiveLoans(@Param("customerId") Long customerId);
}
