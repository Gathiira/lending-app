package com.local.lms.domain.entity;

import com.local.lms.domain.enums.BillingCycleType;
import com.local.lms.domain.enums.LoanType;
import com.local.lms.domain.enums.TenureType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loan_products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString
public class LoanProduct extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "min_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal minAmount;

    @Column(name = "max_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal maxAmount;

    @Column(name = "tenure_value", nullable = false)
    private Integer tenureValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenure_type", nullable = false, columnDefinition = "tenure_type")
    private TenureType tenureType;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, columnDefinition = "loan_type")
    private LoanType loanType = LoanType.LUMP_SUM;

    @Column(name = "installment_count")
    private Integer installmentCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle_type", nullable = false, columnDefinition = "billing_cycle_type")
    private BillingCycleType billingCycleType = BillingCycleType.INDIVIDUAL;

    @Column(name = "grace_period_days", nullable = false)
    private Integer gracePeriodDays = 0;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<ProductFee> fees = new ArrayList<>();
}
