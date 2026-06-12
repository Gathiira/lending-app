package com.local.lms.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "repayments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Repayment extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repayment_reference", nullable = false, unique = true, length = 50)
    private String repaymentReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installment_id")
    private LoanInstallment installment;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "principal_paid", nullable = false)
    @Builder.Default
    private BigDecimal principalPaid = BigDecimal.ZERO;

    @Column(name = "fees_paid", nullable = false)
    @Builder.Default
    private BigDecimal feesPaid = BigDecimal.ZERO;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
