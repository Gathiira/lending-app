package com.local.lms.domain.entity;

import com.local.lms.domain.enums.FeeCalculationMethod;
import com.local.lms.domain.enums.FeeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_fees")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString
public class ProductFee extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProduct product;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, columnDefinition = "fee_type")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private FeeType feeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_method", nullable = false, columnDefinition = "fee_calculation_method")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private FeeCalculationMethod calculationMethod;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /** Number of days after due date before this fee is triggered (for LATE_FEE) */
    @Column(name = "days_after_due", nullable = false)
    private Integer daysAfterDue = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

}
