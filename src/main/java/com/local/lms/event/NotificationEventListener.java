package com.local.lms.event;

import com.local.lms.domain.entity.Notification;
import com.local.lms.domain.enums.NotificationChannel;
import com.local.lms.domain.enums.NotificationStatus;
import com.local.lms.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotification(NotificationEvent event) {
        Notification notification = event.getNotification();

        try {
            dispatch(notification);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to send notification event={} to={}",
                    notification.getEvent(), notification.getRecipient(), e);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
        }

        notificationRepository.save(notification);
    }

    private void dispatch(Notification notification) {
        if (notification.getChannel() == NotificationChannel.SMS) {
            sendSms(notification);
        } else if (notification.getChannel() == NotificationChannel.EMAIL) {
            sendEmail(notification);
        }
    }

    private void sendSms(Notification notification) {
        log.info("[SMS] Would send to {}: {}", notification.getRecipient(), notification.getMessage());
    }

    private void sendEmail(Notification notification) {
        log.info("[EMAIL] Would send to {}: {}", notification.getRecipient(), notification.getMessage());
    }
}
