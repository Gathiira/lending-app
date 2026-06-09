package com.local.lms.repository;

import com.local.lms.domain.entity.LoanInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanInstallmentRepository  extends JpaRepository<LoanInstallment, Long> {

}
