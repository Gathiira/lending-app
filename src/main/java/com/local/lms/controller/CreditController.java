package com.local.lms.controller;

import com.local.lms.dto.request.ApproveCustomerLimitRequest;
import com.local.lms.dto.request.CreditLimitAdjustmentRequest;
import com.local.lms.dto.request.CreditSearchRequest;
import com.local.lms.dto.response.*;
import com.local.lms.service.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/credit")
@RequiredArgsConstructor
@Tag(name = "Credit", description = "Credit management APIs")
public class CreditController extends  BaseController {

    private final CreditService creditService;

    @GetMapping
    @Operation(summary = "Get limit requests")
    public ResponseResult<PaginatedResponse<CreditLimitRequestResponse>> getByLimitRequest(CreditSearchRequest request) {
        return ResponseResult.success(creditService.getPage(request, getPageable()));
    }

    @GetMapping("/limits")
    @Operation(summary = "Get limits")
    public ResponseResult<List<CreditLimitResponse>> getByCreditLimit() {
        return ResponseResult.success(creditService.getCreditLimit());
    }

    @PutMapping("/{id}/update")
    @Operation(summary = "Update customer's credit limit")
    public ResponseResult<CreditLimitRequestResponse> updateCreditLimit(
            @PathVariable Long id,
            @Validated @RequestBody ApproveCustomerLimitRequest request) {
        return ResponseResult.success("Credit limit updated", creditService.updateCreditLimit(id, request));
    }

    @PutMapping("/customer/{customerId}/credit-limit/adjust")
    public ResponseResult<CreditLimitResponse> adjustCreditLimit(
            @PathVariable Long customerId,
            @Validated @RequestBody CreditLimitAdjustmentRequest request) {
        return ResponseResult.success("Credit limit adjusted", creditService.adjustCreditLimit(customerId, request));
    }
}
