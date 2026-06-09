package com.local.lms.repository;

import com.local.lms.domain.entity.LoanFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanFeeRepository extends JpaRepository<LoanFee, Long> {
}
