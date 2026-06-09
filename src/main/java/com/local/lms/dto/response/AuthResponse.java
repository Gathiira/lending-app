package com.local.lms.dto.response;

import com.local.lms.domain.enums.AccountStatus;
import com.local.lms.domain.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuthResponse {
    private Long id;
    private String username;
    private String token;
    private AccountStatus status;
    private UserRole role;
    private LocalDateTime lastLoginAt;
    private CustomerResponse customer;
}
