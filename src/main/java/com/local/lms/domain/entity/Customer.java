package com.local.lms.domain.entity;

import com.local.lms.domain.enums.NotificationChannel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer {

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
    private NotificationChannel preferredChannel = NotificationChannel.EMAIL;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private List<Loan> loans = new ArrayList<>();

    public String getFullName() {
        return firstName + " " + lastName;
    }
}