package com.local.lms.security;

import com.local.lms.domain.entity.Customer;
import com.local.lms.domain.entity.UserAccount;
import com.local.lms.domain.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class JwtContext {

    public UserAccount getCurrentUser() {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        assert auth != null;
        return (UserAccount) auth.getPrincipal();
    }

    public Long getUserId() {
        return getCurrentUser().getId();
    }

    public Customer getCustomer() {
        return getCurrentUser().getCustomer();
    }

    public Long getCustomerId() {
        return getCustomer().getId();
    }

    public String getUsername() {
        return getCurrentUser().getUsername();
    }

    public UserRole getRole() {
        return getCurrentUser().getRole();
    }
}
