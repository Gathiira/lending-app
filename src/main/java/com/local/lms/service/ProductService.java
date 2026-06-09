package com.local.lms.service;

import com.local.lms.dto.request.CreateLoanProductRequest;
import com.local.lms.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(CreateLoanProductRequest request);
    ProductResponse getProduct(Long id);
    List<ProductResponse> getAllProducts();
    List<ProductResponse> getActiveProducts();
    ProductResponse updateProduct(Long id, CreateLoanProductRequest request);
    void deactivateProduct(Long id);
}
