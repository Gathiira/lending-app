package com.local.lms.repository;

import com.local.lms.domain.entity.NotificationTracker;
import com.local.lms.domain.enums.NotificationEventType;
import com.local.lms.domain.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface NotificationTrackerRepository extends JpaRepository<NotificationTracker, Long> {

    Optional<NotificationTracker> findByLoanIdAndEventAndNotificationDate(
            Long loanId, NotificationEventType event, LocalDate notificationDate);

    boolean existsByLoanIdAndEventAndNotificationDate(
            Long loanId, NotificationEventType event, LocalDate notificationDate);
}
