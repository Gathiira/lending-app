package com.local.lms.controller.customer;

import com.local.lms.controller.BaseController;
import com.local.lms.dto.request.CreateCustomerRequest;
import com.local.lms.dto.response.CustomerResponse;
import com.local.lms.dto.response.ResponseResult;
import com.local.lms.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/customer")
@RequiredArgsConstructor
@Tag(name = "Customer Profile", description = "Customer profile management APIs")
public class CustomerProfileController extends BaseController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "Get customer by ID")
    public ResponseResult<CustomerResponse> getById() {
        return ResponseResult.success(customerService.getCustomer(getCustomerId()));
    }

    @PutMapping
    @Operation(summary = "Update customer information")
    public ResponseResult<CustomerResponse> update(@Validated @RequestBody CreateCustomerRequest request) {
        return ResponseResult.success("Customer updated", customerService.updateCustomer(getCustomerId(), request));
    }

}
