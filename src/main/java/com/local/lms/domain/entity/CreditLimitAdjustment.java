package com.local.lms.domain.entity;
import com.local.lms.domain.enums.AdjustmentType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "credit_limit_adjustment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CreditLimitAdjustment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_limit_id", nullable = false)
    private CreditLimit creditLimit;

    private BigDecimal amount;
    private String reason;

    @Enumerated(EnumType.STRING)
    private AdjustmentType type;
}
