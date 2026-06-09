package com.local.lms.repository;

import com.local.lms.domain.entity.NotificationTemplate;
import com.local.lms.domain.enums.NotificationChannel;
import com.local.lms.domain.enums.NotificationEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findByEventAndChannelAndActiveTrue(NotificationEventType event, NotificationChannel channel);
}
