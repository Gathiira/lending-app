package com.local.lms.dto.request;

import com.local.lms.domain.enums.BillingCycleType;
import com.local.lms.domain.enums.LoanType;
import com.local.lms.domain.enums.TenureType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateLoanProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Minimum amount is required")
    @DecimalMin(value = "1.0", message = "Min amount must be positive")
    private BigDecimal minAmount;

    @NotNull(message = "Maximum amount is required")
    @DecimalMin(value = "1.0", message = "Max amount must be positive")
    private BigDecimal maxAmount;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.1", message = "Interest rate must be positive")
    private BigDecimal interestRate;

    @NotNull(message = "Tenure value is required")
    @Min(value = 1, message = "Tenure value must be at least 1")
    private Integer tenureValue;

    @NotNull(message = "Tenure type is required")
    private TenureType tenureType;

    @NotNull(message = "Loan type is required")
    private LoanType loanType;

    private Integer installmentCount;

    private BillingCycleType billingCycleType = BillingCycleType.INDIVIDUAL;

    @Min(value = 0, message = "Grace period days must be >= 0")
    private Integer gracePeriodDays = 0;

    @Valid
    private List<ProductFeeRequest> fees;
}
