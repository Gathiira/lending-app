package com.local.lms.controller;

import com.local.lms.dto.request.CreateCustomerRequest;
import com.local.lms.dto.request.UpdateCustomerLimitRequest;
import com.local.lms.dto.response.ResponseResult;
import com.local.lms.dto.response.CustomerResponse;
import com.local.lms.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer profile management APIs")
public class CustomerController extends  BaseController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(summary = "Create a new customer")
    public ResponseResult<CustomerResponse> create(@Validated @RequestBody CreateCustomerRequest request) {
        return ResponseResult.success("Customer created successfully", customerService.createCustomer(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    public ResponseResult<CustomerResponse> getById(@PathVariable Long id) {
        return ResponseResult.success(customerService.getCustomer(id));
    }

    @GetMapping
    @Operation(summary = "Get all customers")
    public ResponseResult<List<CustomerResponse>> getAll() {
        return ResponseResult.success(customerService.getAllCustomers());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer information")
    public ResponseResult<CustomerResponse> update(
            @PathVariable Long id,
            @Validated @RequestBody CreateCustomerRequest request) {
        return ResponseResult.success("Customer updated", customerService.updateCustomer(id, request));
    }

    @PatchMapping("/{id}/loan-limit")
    @Operation(summary = "Update customer's loan limit")
    public ResponseResult<CustomerResponse> updateLoanLimit(
            @PathVariable Long id,
            @Validated @RequestBody UpdateCustomerLimitRequest request) {
        return ResponseResult.success("Loan limit updated", customerService.updateLoanLimit(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a customer")
    public ResponseResult<Void> deactivate(@PathVariable Long id) {
        customerService.deactivateCustomer(id);
        return ResponseResult.success("Customer deactivated");
    }
}
