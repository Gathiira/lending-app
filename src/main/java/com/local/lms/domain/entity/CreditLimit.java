package com.local.lms.domain.entity;

import com.local.lms.domain.enums.CreditLimitStatus;
import com.local.lms.exceptions.BusinessException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "credit_limit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CreditLimit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private CreditLimitRequest creditLimitRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProduct loanProduct;

    @Column(name = "credit_limit", nullable = false, precision = 16, scale = 2)
    private BigDecimal currentLimit = BigDecimal.ZERO;

    @Column(name = "frozen_limit", nullable = false, precision = 16, scale = 2)
    private BigDecimal frozenLimit = BigDecimal.ZERO;

    @Column(name = "available_limit", nullable = false, precision = 16, scale = 2)
    private BigDecimal availableLimit =  BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "status")
    private CreditLimitStatus status = CreditLimitStatus.ACTIVE;


    public void freeze(BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new BusinessException("Amount must be positive");
        }

        if (availableLimit.compareTo(amount) < 0) {
            throw new BusinessException("Insufficient available limit");
        }

        frozenLimit = frozenLimit.add(amount);
        availableLimit = availableLimit.subtract(frozenLimit);
    }

    public void utilizeFrozenLimit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be greater than zero");
        }
        if (amount.compareTo(frozenLimit) > 0) {
            throw new BusinessException("Utilization amount exceeds frozen limit");
        }
        frozenLimit = frozenLimit.subtract(amount);
    }

    public void restoreLimit(BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new BusinessException("Amount must be positive");
        }
        availableLimit = availableLimit.add(amount);
    }

}
