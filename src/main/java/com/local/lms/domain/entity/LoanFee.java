package com.local.lms.domain.entity;

import com.local.lms.domain.enums.FeeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_fees")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanFee extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_fee_id")
    private ProductFee productFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, columnDefinition = "fee_type")
    private FeeType feeType;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "applied_date", nullable = false)
    private LocalDate appliedDate;

    @Column(nullable = false)
    private Boolean paid = false;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(columnDefinition = "TEXT")
    private String description;
}
