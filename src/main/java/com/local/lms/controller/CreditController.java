package com.local.lms.controller;

import com.local.lms.dto.request.ApproveCustomerLimitRequest;
import com.local.lms.dto.response.CreditLimitRequestResponse;
import com.local.lms.dto.response.CreditLimitResponse;
import com.local.lms.dto.response.CustomerResponse;
import com.local.lms.dto.response.ResponseResult;
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
    public ResponseResult<List<CreditLimitRequestResponse>> getByLimitRequest() {
        return ResponseResult.success(creditService.getLimitRequests());
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
        return ResponseResult.success("Loan limit updated", creditService.updateCreditLimit(id, request));
    }
}
