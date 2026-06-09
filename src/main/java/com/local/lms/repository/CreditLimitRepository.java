package com.local.lms.repository;

import com.local.lms.domain.entity.CreditLimit;
import com.local.lms.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditLimitRepository extends JpaRepository<CreditLimit, Long> {
    boolean existsByCustomer(Customer customer);
    Optional<CreditLimit> findByCustomer(Customer customer);
    Optional<CreditLimit> findByCustomerId(Long customerId);
}
