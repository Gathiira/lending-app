package com.local.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerLimitRequest {
    @NotBlank
    private String fileUrl;
    private String reason;
}
