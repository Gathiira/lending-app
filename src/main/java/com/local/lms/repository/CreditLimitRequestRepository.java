package com.local.lms.repository;

import com.local.lms.domain.entity.CreditLimitRequest;
import com.local.lms.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditLimitRequestRepository extends JpaRepository<CreditLimitRequest, Long>, JpaSpecificationExecutor<CreditLimitRequest> {
    boolean existsByCustomer(Customer customer);
    Optional<CreditLimitRequest> findByCustomer(Customer customer);

    List<CreditLimitRequest> findByCustomerId(Long customer);
}
