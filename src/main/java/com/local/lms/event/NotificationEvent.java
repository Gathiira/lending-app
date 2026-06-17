package com.local.lms.event;

import com.local.lms.domain.entity.Customer;
import com.local.lms.domain.entity.Loan;
import com.local.lms.domain.entity.Notification;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NotificationEvent extends ApplicationEvent {

    private final Notification notification;
    private final Customer customer;
    private final Loan loan;

    public NotificationEvent(Object source, Notification notification, Customer customer, Loan loan) {
        super(source);
        this.notification = notification;
        this.customer = customer;
        this.loan = loan;
    }
}
