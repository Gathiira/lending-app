package com.local.lms.service.impl;

import com.local.lms.domain.entity.LoanProduct;
import com.local.lms.domain.entity.ProductFee;
import com.local.lms.dto.request.CreateLoanProductRequest;
import com.local.lms.dto.request.ProductFeeRequest;
import com.local.lms.dto.response.ProductFeeResponse;
import com.local.lms.dto.response.ProductResponse;
import com.local.lms.exceptions.BusinessException;
import com.local.lms.exceptions.ResourceNotFoundException;
import com.local.lms.repository.LoanProductRepository;
import com.local.lms.repository.ProductFeeRepository;
import com.local.lms.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final LoanProductRepository productRepository;
    private final ProductFeeRepository productFeeRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateLoanProductRequest request) {
        if (productRepository.findByName(request.getName()).isPresent()) {
            throw new BusinessException("Product with name '" + request.getName() + "' already exists");
        }
        validateProductRequest(request);

        LoanProduct product = LoanProduct.builder()
                .name(request.getName())
                .description(request.getDescription())
                .minAmount(request.getMinAmount())
                .maxAmount(request.getMaxAmount())
                .tenureValue(request.getTenureValue())
                .tenureType(request.getTenureType())
                .loanType(request.getLoanType())
                .installmentCount(request.getInstallmentCount())
                .billingCycleType(request.getBillingCycleType())
                .gracePeriodDays(request.getGracePeriodDays())
                .active(true)
                .fees(new ArrayList<>())
                .build();

        if (request.getFees() != null) {
            request.getFees().forEach(feeReq -> {
                ProductFee fee = mapToFeeEntity(feeReq);
                fee.setProduct(product);
                product.getFees().add(fee);
            });
        }

        LoanProduct saved = productRepository.save(product);
        log.info("Created loan product: {} (id={})", saved.getName(), saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return mapToResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getActiveProducts() {
        return productRepository.findByActiveTrue().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, CreateLoanProductRequest request) {
        LoanProduct product = findById(id);
        validateProductRequest(request);

        // Check name uniqueness if changed
        if (!product.getName().equals(request.getName())) {
            productRepository.findByName(request.getName()).ifPresent(existing -> {
                throw new BusinessException("Product with name '" + request.getName() + "' already exists");
            });
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setMinAmount(request.getMinAmount());
        product.setMaxAmount(request.getMaxAmount());
        product.setTenureValue(request.getTenureValue());
        product.setTenureType(request.getTenureType());
        product.setLoanType(request.getLoanType());
        product.setInstallmentCount(request.getInstallmentCount());
        product.setBillingCycleType(request.getBillingCycleType());
        product.setGracePeriodDays(request.getGracePeriodDays());

        // Replace fees
        product.getFees().clear();
        if (request.getFees() != null) {
            request.getFees().forEach(feeReq -> {
                ProductFee fee = mapToFeeEntity(feeReq);
                fee.setProduct(product);
                ProductFee saved = productFeeRepository.save(fee);
                product.getFees().add(saved);
            });
        }

        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deactivateProduct(Long id) {
        LoanProduct product = findById(id);
        product.setActive(false);
        productRepository.save(product);
        log.info("Deactivated loan product id={}", id);
    }

    // ---- helpers ----

    private LoanProduct findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LoanProduct", id));
    }

    private void validateProductRequest(CreateLoanProductRequest request) {
        if (request.getMinAmount().compareTo(request.getMaxAmount()) > 0) {
            throw new BusinessException("Min amount cannot be greater than max amount");
        }
        if (request.getLoanType().name().equals("INSTALLMENT") && request.getInstallmentCount() == null) {
            throw new BusinessException("Installment count is required for INSTALLMENT loan type");
        }
    }

    private ProductFee mapToFeeEntity(ProductFeeRequest req) {
        return ProductFee.builder()
                .feeType(req.getFeeType())
                .calculationMethod(req.getCalculationMethod())
                .amount(req.getAmount())
                .daysAfterDue(req.getDaysAfterDue() != null ? req.getDaysAfterDue() : 0)
                .description(req.getDescription())
                .active(true)
                .build();
    }

    public ProductResponse mapToResponse(LoanProduct product) {
        List<ProductFeeResponse> feeResponses = product.getFees().stream()
                .filter(f -> Boolean.TRUE.equals(f.getActive()))
                .map(f -> ProductFeeResponse.builder()
                        .id(f.getId())
                        .feeType(f.getFeeType())
                        .calculationMethod(f.getCalculationMethod())
                        .amount(f.getAmount())
                        .daysAfterDue(f.getDaysAfterDue())
                        .description(f.getDescription())
                        .build())
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .minAmount(product.getMinAmount())
                .maxAmount(product.getMaxAmount())
                .tenureValue(product.getTenureValue())
                .tenureType(product.getTenureType())
                .loanType(product.getLoanType())
                .installmentCount(product.getInstallmentCount())
                .billingCycleType(product.getBillingCycleType())
                .gracePeriodDays(product.getGracePeriodDays())
                .active(product.getActive())
                .fees(feeResponses)
                .createdAt(product.getCreatedAt())
                .build();
    }
}
