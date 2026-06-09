package com.local.lms.repository;

import com.local.lms.domain.entity.ProductFee;
import com.local.lms.domain.enums.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductFeeRepository extends JpaRepository<ProductFee, Long> {
    List<ProductFee> findByProductIdAndActiveTrue(Long productId);
    List<ProductFee> findByProductIdAndFeeTypeAndActiveTrue(Long productId, FeeType feeType);
}
