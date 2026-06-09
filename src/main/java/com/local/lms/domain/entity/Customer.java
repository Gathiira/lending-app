package com.local.lms.domain.entity;

import com.local.lms.domain.enums.NotificationChannel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "national_id", unique = true, length = 50)
    private String nationalId;

    @Column(name = "credit_score")
    private Integer creditScore = 0;

    @Column(name = "max_loan_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal maxLoanLimit = BigDecimal.ZERO;

    @Column(name = "current_loan_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentLoanLimit = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_channel", nullable = false, columnDefinition = "notification_channel")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private NotificationChannel preferredChannel = NotificationChannel.EMAIL;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private List<Loan> loans = new ArrayList<>();

    public String getFullName() {
        return firstName + " " + lastName;
    }
}