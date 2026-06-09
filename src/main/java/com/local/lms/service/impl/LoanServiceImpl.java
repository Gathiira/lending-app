package com.local.lms.service.impl;

import com.local.lms.domain.entity.*;
import com.local.lms.domain.enums.*;
import com.local.lms.dto.request.CreateLoanRequest;
import com.local.lms.dto.request.RepaymentRequest;
import com.local.lms.dto.response.*;
import com.local.lms.exceptions.BusinessException;
import com.local.lms.exceptions.ResourceNotFoundException;
import com.local.lms.repository.*;
import com.local.lms.service.LoanService;
import com.local.lms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;
    private final LoanFeeRepository loanFeeRepository;
    private final RepaymentRepository repaymentRepository;
    private final CustomerRepository customerRepository;
    private final LoanProductRepository productRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public LoanResponse createLoan(CreateLoanRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId()));

        LoanProduct product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("LoanProduct", request.getProductId()));

        if (!product.getActive()) throw new BusinessException("Loan product is not active");
        if (!customer.getActive()) throw new BusinessException("Customer account is not active");

        validateLoanAmount(request.getAmount(), product, customer);

        LocalDate disbursementDate = LocalDate.now();
        LocalDate dueDate = calculateDueDate(disbursementDate, product);

        // Deduct from customer limit
        customer.setCurrentLoanLimit(customer.getCurrentLoanLimit().subtract(request.getAmount()));
        customerRepository.save(customer);

        Loan loan = Loan.builder()
                .loanReference(generateReference())
                .customer(customer)
                .product(product)
                .principalAmount(request.getAmount())
                .outstandingBalance(request.getAmount())
                .loanType(product.getLoanType())
                .status(LoanStatus.OPEN)
                .billingCycleType(request.getBillingCycleType() != null ? request.getBillingCycleType() : product.getBillingCycleType())
                .consolidatedDueDate(request.getDueDate())
                .disbursementDate(disbursementDate)
                .dueDate(dueDate)
                .notes(request.getNotes())
                .build();

        loanRepository.save(loan);

        // Apply service fee
        applyServiceFee(loan, product);

        // Generate installments for installment loans
        if (product.getLoanType() == LoanType.INSTALLMENT) {
            generateInstallments(loan, product);
        }

        loanRepository.save(loan);

        log.info("Created loan {} for customer {} amount={}", loan.getLoanReference(), customer.getEmail(), request.getAmount());
        notificationService.sendNotification(customer, loan, NotificationEventType.LOAN_CREATED);

        return mapToResponse(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanResponse getLoan(Long id) {
        return mapToResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public LoanResponse getLoanByReference(String reference) {
        Loan loan = loanRepository.findByLoanReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with reference: " + reference));
        return mapToResponse(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanResponse> getCustomerLoans(Long customerId) {
        return loanRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanResponse> getLoans() {
        return loanRepository.findAll().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RepaymentResponse makeRepayment(RepaymentRequest request) {
        Loan loan = findById(request.getLoanId());

        if (!loan.isActive()) {
            throw new BusinessException("Cannot make repayment on a " + loan.getStatus() + " loan");
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Repayment amount must be positive");
        }

        BigDecimal paymentAmount = request.getAmount();
        BigDecimal feesSettled = BigDecimal.ZERO;
        BigDecimal principalSettled = BigDecimal.ZERO;

        // First settle unpaid fees
        List<LoanFee> unpaidFees = loanFeeRepository.findByLoanIdAndPaidFalse(loan.getId());
        for (LoanFee fee : unpaidFees) {
            if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) break;
            if (paymentAmount.compareTo(fee.getAmount()) >= 0) {
                paymentAmount = paymentAmount.subtract(fee.getAmount());
                feesSettled = feesSettled.add(fee.getAmount());
                fee.setPaid(true);
                fee.setPaidDate(LocalDate.now());
                loanFeeRepository.save(fee);
            }
        }

        // Then reduce principal
        principalSettled = paymentAmount.min(loan.getOutstandingBalance());
        loan.setOutstandingBalance(loan.getOutstandingBalance().subtract(principalSettled));

        // Update installment if specified
        LoanInstallment installment = null;
        if (request.getInstallmentId() != null) {
            installment = installmentRepository.findById(request.getInstallmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Installment", request.getInstallmentId()));
            BigDecimal newInstallmentBalance = installment.getOutstandingAmount().subtract(principalSettled);
            installment.setOutstandingAmount(newInstallmentBalance.max(BigDecimal.ZERO));
            if (installment.getOutstandingAmount().compareTo(BigDecimal.ZERO) == 0) {
                installment.setStatus(LoanStatus.CLOSED);
                installment.setPaidDate(LocalDate.now());
            }
            installmentRepository.save(installment);
        }

        // Close loan if fully paid
        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(LoanStatus.CLOSED);
            loan.setClosedDate(LocalDate.now());
            // Restore customer limit
            Customer customer = loan.getCustomer();
            customer.setCurrentLoanLimit(customer.getCurrentLoanLimit().add(loan.getPrincipalAmount()));
            customerRepository.save(customer);
            notificationService.sendNotification(customer, loan, NotificationEventType.LOAN_CLOSED);
        }

        LocalDate paymentDate = request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now();
        Repayment repayment = Repayment.builder()
                .repaymentReference("RPY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .loan(loan)
                .installment(installment)
                .amount(request.getAmount())
                .principalPaid(principalSettled)
                .feesPaid(feesSettled)
                .paymentDate(paymentDate)
                .notes(request.getNotes())
                .build();

        loanRepository.save(loan);
        Repayment saved = repaymentRepository.save(repayment);

        notificationService.sendNotification(loan.getCustomer(), loan, NotificationEventType.LOAN_REPAYMENT);
        log.info("Repayment {} of {} made on loan {}", saved.getRepaymentReference(), request.getAmount(), loan.getLoanReference());

        return mapRepaymentToResponse(saved);
    }

    @Override
    @Transactional
    public LoanResponse cancelLoan(Long id) {
        Loan loan = findById(id);
        if (loan.getStatus() != LoanStatus.OPEN) {
            throw new BusinessException("Only OPEN loans can be cancelled");
        }
        loan.setStatus(LoanStatus.CANCELLED);
        // Restore limit
        Customer customer = loan.getCustomer();
        customer.setCurrentLoanLimit(customer.getCurrentLoanLimit().add(loan.getPrincipalAmount()));
        customerRepository.save(customer);
        loanRepository.save(loan);
        notificationService.sendNotification(customer, loan, NotificationEventType.LOAN_CANCELLED);
        return mapToResponse(loan);
    }

    @Override
    @Transactional
    public LoanResponse writeOffLoan(Long id) {
        Loan loan = findById(id);
        if (loan.getStatus() != LoanStatus.OVERDUE) {
            throw new BusinessException("Only OVERDUE loans can be written off");
        }
        loan.setStatus(LoanStatus.WRITTEN_OFF);
        loan.setWrittenOffDate(LocalDate.now());
        loanRepository.save(loan);
        notificationService.sendNotification(loan.getCustomer(), loan, NotificationEventType.LOAN_WRITTEN_OFF);
        return mapToResponse(loan);
    }

    @Override
    @Transactional
    public void processOverdueLoans() {
        LocalDate today = LocalDate.now();
        List<Loan> openOverdue = loanRepository.findOpenLoansOverdue(today);
        log.info("Sweep: found {} open loans past due", openOverdue.size());

        for (Loan loan : openOverdue) {
            loan.setStatus(LoanStatus.OVERDUE);

            // Apply late fees from the product configuration
            loan.getProduct().getFees().stream()
                    .filter(f -> f.getFeeType() == FeeType.LATE_FEE && Boolean.TRUE.equals(f.getActive()))
                    .filter(f -> {
                        long daysOverdue = today.toEpochDay() - loan.getDueDate().toEpochDay();
                        return daysOverdue >= f.getDaysAfterDue();
                    })
                    .filter(f -> !loanFeeRepository.existsByLoanIdAndFeeTypeAndPaidFalse(loan.getId(), FeeType.LATE_FEE))
                    .forEach(f -> {
                        BigDecimal feeAmount = calculateFeeAmount(f, loan.getOutstandingBalance());
                        LoanFee loanFee = LoanFee.builder()
                                .loan(loan)
                                .productFee(f)
                                .feeType(FeeType.LATE_FEE)
                                .amount(feeAmount)
                                .appliedDate(today)
                                .paid(false)
                                .description("Late fee applied")
                                .build();
                        loanFeeRepository.save(loanFee);
                        notificationService.sendNotification(loan.getCustomer(), loan, NotificationEventType.LATE_FEE_APPLIED);
                        log.info("Applied late fee {} to loan {}", feeAmount, loan.getLoanReference());
                    });

            loanRepository.save(loan);
            notificationService.sendNotification(loan.getCustomer(), loan, NotificationEventType.LOAN_OVERDUE);
        }
    }

    @Override
    @Transactional
    public void applyDailyFees() {
        LocalDate today = LocalDate.now();
        List<Loan> activeLoans = loanRepository.findOpenLoansOverdue(today);
        // Also get all open loans with daily fee product
        loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == LoanStatus.OPEN || l.getStatus() == LoanStatus.OVERDUE)
                .forEach(loan -> {
                    loan.getProduct().getFees().stream()
                            .filter(f -> f.getFeeType() == FeeType.DAILY_FEE && Boolean.TRUE.equals(f.getActive()))
                            .forEach(f -> {
                                BigDecimal dailyFeeAmount = calculateFeeAmount(f, loan.getOutstandingBalance());
                                LoanFee loanFee = LoanFee.builder()
                                        .loan(loan)
                                        .productFee(f)
                                        .feeType(FeeType.DAILY_FEE)
                                        .amount(dailyFeeAmount)
                                        .appliedDate(today)
                                        .paid(false)
                                        .description("Daily fee for " + today)
                                        .build();
                                loanFeeRepository.save(loanFee);
                                // Add to outstanding
                                loan.setOutstandingBalance(loan.getOutstandingBalance().add(dailyFeeAmount));
                                loanRepository.save(loan);
                            });
                });
    }

    // ---- private helpers ----

    private Loan findById(Long id) {
        return loanRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Loan", id));
    }

    private void validateLoanAmount(BigDecimal amount, LoanProduct product, Customer customer) {
        if (amount.compareTo(product.getMinAmount()) < 0) {
            throw new BusinessException("Amount is below the product minimum of " + product.getMinAmount());
        }
        if (amount.compareTo(product.getMaxAmount()) > 0) {
            throw new BusinessException("Amount exceeds product maximum of " + product.getMaxAmount());
        }
        if (amount.compareTo(customer.getCurrentLoanLimit()) > 0) {
            throw new BusinessException("Amount exceeds customer's available limit of " + customer.getCurrentLoanLimit());
        }
    }

    private LocalDate calculateDueDate(LocalDate from, LoanProduct product) {
        return switch (product.getTenureType()) {
            case DAYS -> from.plusDays(product.getTenureValue());
            case MONTHS -> from.plusMonths(product.getTenureValue());
        };
    }

    private void applyServiceFee(Loan loan, LoanProduct product) {
        product.getFees().stream()
                .filter(f -> f.getFeeType() == FeeType.SERVICE_FEE && Boolean.TRUE.equals(f.getActive()))
                .forEach(f -> {
                    BigDecimal feeAmount = calculateFeeAmount(f, loan.getPrincipalAmount());
                    LoanFee loanFee = LoanFee.builder()
                            .loan(loan)
                            .productFee(f)
                            .feeType(FeeType.SERVICE_FEE)
                            .amount(feeAmount)
                            .appliedDate(loan.getDisbursementDate())
                            .paid(false)
                            .description(f.getDescription() != null ? f.getDescription() : "Service fee")
                            .build();
                    loanFeeRepository.save(loanFee);
                    loan.setOutstandingBalance(loan.getOutstandingBalance().add(feeAmount));
                });
    }

    private BigDecimal calculateFeeAmount(ProductFee fee, BigDecimal principal) {
        if (fee.getCalculationMethod() == FeeCalculationMethod.FIXED) {
            return fee.getAmount();
        } else {
            return principal.multiply(fee.getAmount())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }
    }

    private void generateInstallments(Loan loan, LoanProduct product) {
        int count = product.getInstallmentCount();
        BigDecimal installmentAmount = loan.getPrincipalAmount()
                .divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);

        for (int i = 1; i <= count; i++) {
            LocalDate installmentDue = switch (product.getTenureType()) {
                case MONTHS -> loan.getDisbursementDate().plusMonths(i);
                case DAYS -> loan.getDisbursementDate().plusDays((long) loan.getProduct().getTenureValue() / count * i);
            };

            LoanInstallment installment = LoanInstallment.builder()
                    .loan(loan)
                    .installmentNumber(i)
                    .principalAmount(installmentAmount)
                    .outstandingAmount(installmentAmount)
                    .dueDate(installmentDue)
                    .status(LoanStatus.OPEN)
                    .build();
            loan.getInstallments().add(installment);
        }
    }

    private String generateReference() {
        return "LN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public LoanResponse mapToResponse(Loan loan) {
        List<InstallmentResponse> installmentResponses = loan.getInstallments().stream()
                .map(i -> InstallmentResponse.builder()
                        .id(i.getId())
                        .installmentNumber(i.getInstallmentNumber())
                        .principalAmount(i.getPrincipalAmount())
                        .outstandingAmount(i.getOutstandingAmount())
                        .dueDate(i.getDueDate())
                        .paidDate(i.getPaidDate())
                        .status(i.getStatus())
                        .build())
                .collect(Collectors.toList());

        List<LoanFeeResponse> feeResponses = loan.getFees().stream()
                .map(f -> LoanFeeResponse.builder()
                        .id(f.getId())
                        .feeType(f.getFeeType())
                        .amount(f.getAmount())
                        .appliedDate(f.getAppliedDate())
                        .paid(f.getPaid())
                        .paidDate(f.getPaidDate())
                        .description(f.getDescription())
                        .build())
                .collect(Collectors.toList());

        return LoanResponse.builder()
                .id(loan.getId())
                .loanReference(loan.getLoanReference())
                .customerId(loan.getCustomer().getId())
                .customerName(loan.getCustomer().getFullName())
                .productId(loan.getProduct().getId())
                .productName(loan.getProduct().getName())
                .principalAmount(loan.getPrincipalAmount())
                .outstandingBalance(loan.getOutstandingBalance())
                .loanType(loan.getLoanType())
                .loanStatus(loan.getStatus())
                .billingCycleType(loan.getBillingCycleType())
                .dueDate(loan.getConsolidatedDueDate())
                .disbursementDate(loan.getDisbursementDate())
                .dueDate(loan.getDueDate())
                .closedDate(loan.getClosedDate())
                .notes(loan.getNotes())
                .installments(installmentResponses)
                .fees(feeResponses)
                .createdAt(loan.getCreatedAt())
                .build();
    }

    private RepaymentResponse mapRepaymentToResponse(Repayment r) {
        return RepaymentResponse.builder()
                .id(r.getId())
                .repaymentReference(r.getRepaymentReference())
                .loanId(r.getLoan().getId())
                .loanReference(r.getLoan().getLoanReference())
                .amount(r.getAmount())
                .principalPaid(r.getPrincipalPaid())
                .feesPaid(r.getFeesPaid())
                .paymentDate(r.getPaymentDate())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
