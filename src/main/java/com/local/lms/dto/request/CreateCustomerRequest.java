package com.local.lms.dto.request;

import com.local.lms.domain.enums.NotificationChannel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateCustomerRequest {
    @NotBlank(message = "First name is required")
    private String firstName;
    @NotBlank(message = "Last Name is required")
    private String lastName;
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String phoneNumber;
    @NotBlank
    private String nationalId;
    @NotBlank
    private String password;

    private Integer creditScore = 0;

    private BigDecimal maxLoanLimit;

    private NotificationChannel preferredChannel = NotificationChannel.EMAIL;
}
