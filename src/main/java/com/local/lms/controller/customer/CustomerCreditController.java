package com.local.lms.controller.customer;

import com.local.lms.controller.BaseController;
import com.local.lms.dto.request.CustomerLimitRequest;
import com.local.lms.dto.response.CreditLimitRequestResponse;
import com.local.lms.dto.response.CreditLimitResponse;
import com.local.lms.dto.response.ResponseResult;
import com.local.lms.service.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/customer")
@RequiredArgsConstructor
@Tag(name = "Customer Credit", description = "Customer-facing credit limit APIs")
public class CustomerCreditController extends BaseController {

    private final CreditService creditService;

    @PostMapping("/apply-limit")
    @Operation(summary = "apply credit limit")
    public ResponseResult<CreditLimitRequestResponse> applyCreditLimit(@Validated @RequestBody CustomerLimitRequest request) {
        return ResponseResult.success("Credit limit Applied", creditService.applyLimit(getCustomerId(), request));
    }

    @GetMapping("/credit-limit")
    @Operation(summary = "get credit limit")
    public ResponseResult<CreditLimitResponse> creditLimit() {
        return ResponseResult.success(creditService.getCustomerCreditLimit(getCustomerId()));
    }

    @GetMapping("/limit-request")
    @Operation(summary = "get credit limit request")
    public ResponseResult<List<CreditLimitRequestResponse>> limitRequest() {
        return ResponseResult.success(creditService.getLimitRequests(getCustomerId()));
    }

}
