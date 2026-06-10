package com.local.lms.service;

import com.local.lms.domain.entity.LoanProduct;
import com.local.lms.dto.request.CreateLoanProductRequest;
import com.local.lms.dto.request.ProductSearchRequest;
import com.local.lms.dto.response.PaginatedResponse;
import com.local.lms.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(CreateLoanProductRequest request);
    ProductResponse getProduct(Long id);
    List<ProductResponse> getAllProducts();
    List<ProductResponse> getActiveProducts();
    ProductResponse updateProduct(Long id, CreateLoanProductRequest request);
    void deactivateProduct(Long id);
    PaginatedResponse<ProductResponse> getPage(ProductSearchRequest request, Pageable pageable);
}
