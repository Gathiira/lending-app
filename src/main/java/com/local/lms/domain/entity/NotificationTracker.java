package com.local.lms.domain.entity;

import com.local.lms.domain.enums.NotificationEventType;
import com.local.lms.domain.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_tracker",
        uniqueConstraints = @UniqueConstraint(columnNames = {"loan_id", "event", "notification_date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationTracker extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationEventType event;

    @Column(name = "notification_date", nullable = false)
    private LocalDate notificationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
