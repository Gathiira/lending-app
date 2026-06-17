package com.local.lms.repository;

import com.local.lms.domain.entity.Loan;
import com.local.lms.domain.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long>, JpaSpecificationExecutor<Loan> {

    Optional<Loan> findByLoanReference(String loanReference);

    Optional<Loan> findByIdAndCustomerId(Long id,  Long customerId);

    List<Loan> findByCustomerId(Long customerId);

    List<Loan> findByCustomerIdAndStatus(Long customerId, LoanStatus status);

    /** Check whether a customer has any active loans */
    @Query("SELECT COUNT(l) > 0 FROM Loan l WHERE l.customer.id = :customerId AND l.status IN ('OPEN', 'OVERDUE')")
    boolean customerHasActiveLoans(@Param("customerId") Long customerId);

    // ========================================================================
    // Atomic batch-claim queries using FOR UPDATE SKIP LOCKED
    // Each instance competes for rows; claimed rows are locked until commit.
    // Must be called inside @Transactional.
    // ========================================================================

    @Query(value = """
            SELECT l.id FROM loans l
            WHERE l.status = 'OPEN' AND l.due_date < :today
            ORDER BY l.id
            FOR UPDATE SKIP LOCKED
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> claimOverdueLoans(@Param("today") LocalDate today, @Param("limit") int limit);

    @Query(value = """
            SELECT l.id FROM loans l
            WHERE l.status IN ('OPEN', 'OVERDUE')
            ORDER BY l.id
            FOR UPDATE SKIP LOCKED
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> claimActiveLoans(@Param("limit") int limit);

    @Query(value = """
            SELECT l.id FROM loans l
            WHERE l.status IN ('OPEN', 'OVERDUE')
              AND l.due_date BETWEEN :from AND :to
            ORDER BY l.id
            FOR UPDATE SKIP LOCKED
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> claimLoansDueBetween(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                    @Param("limit") int limit);
}
