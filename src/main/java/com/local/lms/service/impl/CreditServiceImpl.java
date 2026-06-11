package com.local.lms.service.impl;

import com.local.lms.domain.entity.*;
import com.local.lms.domain.enums.AdjustmentType;
import com.local.lms.domain.enums.ApprovalStatus;
import com.local.lms.domain.enums.CreditLimitStatus;
import com.local.lms.dto.request.ApproveCustomerLimitRequest;
import com.local.lms.dto.request.CreditLimitAdjustmentRequest;
import com.local.lms.dto.request.CreditSearchRequest;
import com.local.lms.dto.request.CustomerLimitRequest;
import com.local.lms.dto.response.CreditLimitRequestResponse;
import com.local.lms.dto.response.CreditLimitResponse;
import com.local.lms.dto.response.PaginatedResponse;
import com.local.lms.exceptions.BusinessException;
import com.local.lms.exceptions.ExceptionAssert;
import com.local.lms.exceptions.ResourceNotFoundException;
import com.local.lms.repository.CreditLimitAdjustmentRepository;
import com.local.lms.repository.CreditLimitRepository;
import com.local.lms.repository.CreditLimitRequestRepository;
import com.local.lms.repository.LoanProductRepository;
import com.local.lms.security.JwtContext;
import com.local.lms.service.CreditService;
import com.local.lms.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditServiceImpl extends BaseServiceImpl<CreditLimitRequest> implements CreditService {

    private final CustomerService customerService;
    private final CreditLimitRequestRepository creditLimitRequestRepository;
    private final LoanProductRepository loanProductRepository;
    private final CreditLimitRepository creditLimitRepository;
    private final JwtContext authContext;
    private final ProductServiceImpl productService;
    private final CreditLimitAdjustmentRepository creditLimitAdjustmentRepository;

    @Override
    public List<CreditLimitRequestResponse> getLimitRequests() {
        return creditLimitRequestRepository.findAll().stream().map(this::mapToLimitRequestResponse).collect(Collectors.toList());
    }

    @Override
    public PaginatedResponse<CreditLimitRequestResponse> getPage(CreditSearchRequest request, Pageable pageable){
        Page<CreditLimitRequest> page = creditLimitRequestRepository.findAll(getSpecifications(request), pageable);
        return toResponse(page, this::mapToLimitRequestResponse);
    }

    @Override
    public List<CreditLimitRequestResponse> getLimitRequests(Long customerId) {
        return creditLimitRequestRepository.findByCustomerId(customerId).stream().map(this::mapToLimitRequestResponse).collect(Collectors.toList());
    }

    @Override
    public  List<CreditLimitResponse> getCreditLimit() {
        return creditLimitRepository.findAll().stream().map(this::mapToLimitResponse).collect(Collectors.toList());
    }

    @Override
    public  CreditLimitResponse getCustomerCreditLimit(Long customerId) {
        CreditLimit creditLimit = creditLimitRepository.findByCustomerId(customerId).orElseThrow(() -> new BusinessException("Credit limit not found"));
        return mapToLimitResponse(creditLimit);
    }

    @Override
    @Transactional
    public CreditLimitRequestResponse applyLimit(Long id, CustomerLimitRequest request) {
        Customer customer = customerService.findById(id);
        CreditLimitRequest existing = creditLimitRequestRepository.findByCustomer(customer).orElse(null);
        if (existing != null) {
            return mapToLimitRequestResponse(existing);
        }
        CreditLimitRequest creditLimitRequest = CreditLimitRequest
                .builder()
                .fileUrl(request.getFileUrl())
                .customer(customer)
                .approvedLimit(BigDecimal.ZERO)
                .status(ApprovalStatus.PENDING)
                .build();
        return mapToLimitRequestResponse(creditLimitRequestRepository.save(creditLimitRequest));
    }

    @Override
    @Transactional
    public CreditLimitRequestResponse updateCreditLimit(Long id, ApproveCustomerLimitRequest request) {
        CreditLimitRequest limitRequest = creditLimitRequestRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("CreditLimitRequest", id));
        if (!limitRequest.getStatus().equals(ApprovalStatus.PENDING)) {
            ExceptionAssert.throwException(limitRequest.getStatus() + " Limit request cannot be updated");
        }

        LoanProduct loanProduct = loanProductRepository.findById(request.getProductId()).orElseThrow(() -> new ResourceNotFoundException("Loan Product", request.getProductId()));
        limitRequest.setStatus(request.getStatus());
        limitRequest.setReason(request.getReason());
        limitRequest.setReviewNotes(request.getReason());
        limitRequest.setReviewedAt(LocalDateTime.now());
        limitRequest.setReviewedBy(authContext.getCurrentUser());
        CreditLimitRequest saved = creditLimitRequestRepository.save(limitRequest);

        //save credit limit
        if (saved.getStatus().equals(ApprovalStatus.APPROVED)) {
            CreditLimit creditLimit = creditLimitRepository.findByCustomer(saved.getCustomer()).orElse(null);
            if (creditLimit == null) {
                creditLimit = CreditLimit
                        .builder()
                        .customer(saved.getCustomer())
                        .loanProduct(loanProduct)
                        .creditLimitRequest(limitRequest)
                        .availableLimit(request.getMaxLoanLimit())
                        .currentLimit(request.getMaxLoanLimit())
                        .frozenLimit(BigDecimal.ZERO)
                        .status(CreditLimitStatus.ACTIVE)
                        .build();
            }
            creditLimitRepository.save(creditLimit);
        }
        return mapToLimitRequestResponse(saved);
    }

    @Override
    @Transactional
    public CreditLimitResponse adjustCreditLimit(Long customerId, CreditLimitAdjustmentRequest request) {
        CreditLimit creditLimit = creditLimitRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("CreditLimit", customerId));

        if (!creditLimit.getStatus().equals(CreditLimitStatus.ACTIVE)) {
            ExceptionAssert.throwException("Cannot adjust a credit limit that is not ACTIVE");
        }

        BigDecimal amount = request.getAmount();

        if (request.getType() == AdjustmentType.INCREASE) {
            creditLimit.setCurrentLimit(creditLimit.getCurrentLimit().add(amount));
            creditLimit.setAvailableLimit(creditLimit.getAvailableLimit().add(amount));

        } else if (request.getType() == AdjustmentType.DECREASE) {
            BigDecimal newCurrent = creditLimit.getCurrentLimit().subtract(amount);
            BigDecimal newAvailable = creditLimit.getAvailableLimit().subtract(amount);

            if (newCurrent.compareTo(BigDecimal.ZERO) < 0) {
                ExceptionAssert.throwException("Decrease amount exceeds current limit");
            }
            if (newAvailable.compareTo(BigDecimal.ZERO) < 0) {
                ExceptionAssert.throwException("Decrease amount exceeds available limit, customer has outstanding utilization");
            }

            creditLimit.setCurrentLimit(newCurrent);
            creditLimit.setAvailableLimit(newAvailable);
        }

        CreditLimit saved = creditLimitRepository.save(creditLimit);

        // persist adjustment record
        CreditLimitAdjustment adjustment = CreditLimitAdjustment.builder()
                .creditLimit(saved)
                .amount(amount)
                .type(request.getType())
                .reason(request.getReason())
                .build();
        creditLimitAdjustmentRepository.save(adjustment);

        log.info("Credit limit adjusted [customerId={}, type={}, amount={}, adjustedBy={}]",
                customerId, request.getType(), amount, authContext.getCurrentUser());

        return mapToLimitResponse(saved);
    }

    // ---- helpers ----
    public CreditLimitRequestResponse mapToLimitRequestResponse(CreditLimitRequest c) {
        return CreditLimitRequestResponse.builder()
                .id(c.getId())
                .reviewedAt(c.getReviewedAt())
                .reviewNotes(c.getReviewNotes())
                .reason(c.getReason())
                .approvedLimit(c.getApprovedLimit())
                .status(c.getStatus())
                .fileUrl(c.getFileUrl())
                .createdAt(c.getCreatedAt())
                .build();
    }

    // ---- helpers ----
    public CreditLimitResponse mapToLimitResponse(CreditLimit c) {
        return CreditLimitResponse.builder()
                .id(c.getId())
                .loanProduct(productService.mapToResponse(c.getLoanProduct()))
                .availableLimit(c.getAvailableLimit())
                .currentLimit(c.getCurrentLimit())
                .frozenLimit(c.getFrozenLimit())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
