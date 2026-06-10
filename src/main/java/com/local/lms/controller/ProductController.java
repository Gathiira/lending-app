package com.local.lms.controller;

import com.local.lms.domain.entity.LoanProduct;
import com.local.lms.dto.request.CreateLoanProductRequest;
import com.local.lms.dto.request.ProductSearchRequest;
import com.local.lms.dto.response.PaginatedResponse;
import com.local.lms.dto.response.ResponseResult;
import com.local.lms.dto.response.ProductResponse;
import com.local.lms.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Loan product management APIs")
public class ProductController extends  BaseController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create a new loan product")
    public ResponseResult<ProductResponse> create(@Validated @RequestBody CreateLoanProductRequest request) {
        return ResponseResult.success("Product created successfully", productService.createProduct(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan product by ID")
    public ResponseResult<ProductResponse> getById(@PathVariable Long id) {
        return ResponseResult.success(productService.getProduct(id));
    }

    @GetMapping
    @Operation(summary = "Get all loan products")
    public ResponseResult<PaginatedResponse<ProductResponse>> getAll(ProductSearchRequest searchRequest) {
        return ResponseResult.success(productService.getPage(searchRequest, getPageable()));
    }


//    @GetMapping
//    @Operation(summary = "Get all loan products")
//    public ResponseResult<List<ProductResponse>> getAll() {
//        List<ProductResponse> products = productService.getAllProducts();
//        return ResponseResult.success(products);
//    }


    @PutMapping("/{id}")
    @Operation(summary = "Update a loan product")
    public ResponseResult<ProductResponse> update(
            @PathVariable Long id,
            @Validated @RequestBody CreateLoanProductRequest request) {
        return ResponseResult.success("Product updated successfully",productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a loan product")
    public ResponseResult<Void> deactivate(@PathVariable Long id) {
        productService.deactivateProduct(id);
        return ResponseResult.success("Product deactivated");
    }
}
