package com.local.lms.dto.response;

import com.local.lms.domain.enums.NotificationChannel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CustomerResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String nationalId;
    private Integer creditScore;
    private BigDecimal maxLoanLimit;
    private BigDecimal currentLoanLimit;
    private NotificationChannel preferredChannel;
    private Boolean active;
    private LocalDateTime createdAt;
}
