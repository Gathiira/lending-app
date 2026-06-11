package com.local.lms.repository;

import com.local.lms.domain.entity.CreditLimitAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditLimitAdjustmentRepository extends JpaRepository<CreditLimitAdjustment, Long> {
    List<CreditLimitAdjustment> findByCreditLimitId(Long creditLimitId);
}
