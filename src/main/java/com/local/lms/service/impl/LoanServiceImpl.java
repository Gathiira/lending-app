package com.local.lms.service.impl;

import com.local.lms.domain.entity.*;
import com.local.lms.domain.enums.*;
import com.local.lms.dto.request.*;
import com.local.lms.dto.response.*;
import com.local.lms.exceptions.BusinessException;
import com.local.lms.exceptions.ResourceNotFoundException;
import com.local.lms.repository.*;
import com.local.lms.service.LoanService;
import com.local.lms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Imperative layer: owns transactions, persistence, and notifications.
 *
 * <p>All financial calculations are delegated to {@link LoanCalculator}.
 * Nothing in this class performs arithmetic or generates dates directly;
 * it only wires together repositories, domain entities, and side-effects
 * (DB writes, notifications, logging).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoanServiceImpl extends BaseServiceImpl<Loan> implements LoanService {

    private final LoanRepository             loanRepository;
    private final LoanInstallmentRepository  installmentRepository;
    private final LoanFeeRepository          loanFeeRepository;
    private final RepaymentRepository        repaymentRepository;
    private final CustomerRepository         customerRepository;
    private final LoanProductRepository      productRepository;
    private final NotificationService        notificationService;
    private final CreditLimitRepository      creditLimitRepository;

    // =========================================================================
    // Loan creation
    // =========================================================================

    @Transactional
    protected LoanResponse persistLoan(BigDecimal amount, LoanProduct product,
                                       Customer customer, String notes) {
        if (!product.getActive()) throw new BusinessException("Loan product is not active");
        if (!customer.getActive()) throw new BusinessException("Customer account is not active");

        // --- pure calculation ---
        LoanCalculator.validateLoanAmount(amount, product);

        LocalDate disbursementDate   = LocalDate.now();
        LocalDate dueDate            = LoanCalculator.calculateDueDate(disbursementDate, product);
        BigDecimal openingBalance    = LoanCalculator.calculateOpeningOutstandingBalance(amount, product);

        // --- persistence ---
        Loan loan = Loan.builder()
                .loanReference(LoanCalculator.generateLoanReference())
                .customer(customer)
                .product(product)
                .principalAmount(amount)
                .outstandingBalance(openingBalance)   // pre-calculated, not incremented inline
                .loanType(product.getLoanType())
                .status(LoanStatus.OPEN)
                .billingCycleType(product.getBillingCycleType())
                .disbursementDate(disbursementDate)
                .dueDate(dueDate)
                .notes(notes)
                .build();

        loanRepository.save(loan);

        persistServiceFees(loan, product);
        persistInterestFee(loan, product);

        if (product.getLoanType() == LoanType.INSTALLMENT) {
            persistInstallments(loan, product);
        }

        loanRepository.saveAndFlush(loan);

        log.info("Created loan {} for customer {} amount={}",
                loan.getLoanReference(), customer.getEmail(), amount);
        notificationService.sendNotification(customer, loan, NotificationEventType.LOAN_CREATED);

        return mapToResponse(loan);
    }

    @Override
    @Transactional
    public LoanResponse createLoan(CreateLoanRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId()));
        LoanProduct product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("LoanProduct", request.getProductId()));

        return persistLoan(request.getAmount(), product, customer, request.getNotes());
    }

    @Override
    @Transactional
    public LoanResponse applyLoan(ApplyLoanRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId()));

        CreditLimit creditLimit = creditLimitRepository.findByCustomer(customer)
                .orElseThrow(() -> new BusinessException("Customer does not have an active credit limit"));

        creditLimit.freeze(request.getAmount());

        LoanResponse loanResponse = persistLoan(
                request.getAmount(), creditLimit.getLoanProduct(), customer, request.getNotes());

        creditLimit.utilizeFrozenLimit(loanResponse.getPrincipalAmount());
        creditLimitRepository.save(creditLimit);

        return loanResponse;
    }

    // =========================================================================
    // Queries
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public LoanResponse getLoan(Long id) {
        return mapToResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public LoanResponse getLoan(Long id, Long customerId) {
        return mapToResponse(findByIdAndCustomer(id, customerId));
    }

    @Override
    @Transactional(readOnly = true)
    public LoanResponse getLoanByReference(String reference) {
        Loan loan = loanRepository.findByLoanReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loan not found with reference: " + reference));
        return mapToResponse(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanResponse> getCustomerLoans(Long customerId) {
        return loanRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanResponse> getLoans() {
        return loanRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<LoanResponse> getPage(LoanSearchRequest request, Pageable pageable) {
        Page<Loan> page = loanRepository.findAll(getSpecifications(request), pageable);
        return toResponse(page, this::mapToResponse);
    }

    // =========================================================================
    // Repayment
    // =========================================================================

    @Override
    @Transactional
    public RepaymentResponse makeRepayment(RepaymentRequest request) {
        Loan loan = findByIdAndCustomer(request.getLoanId(), request.getCustomerId());

        if (!loan.isActive()) {
            throw new BusinessException("Cannot make repayment on a " + loan.getStatus() + " loan");
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Repayment amount must be positive");
        }
        if (request.getAmount().compareTo(loan.getOutstandingBalance()) > 0) {
            throw new BusinessException(
                    "Repayment amount cannot exceed the outstanding balance of "
                            + loan.getOutstandingBalance());
        }

        // --- pure calculation ---
        List<LoanFee> unpaidFees = loanFeeRepository.findByLoanIdAndPaidFalse(loan.getId());
        List<BigDecimal> unpaidFeeAmounts = unpaidFees.stream()
                .map(LoanFee::getAmount)
                .collect(Collectors.toList());

        LoanCalculator.RepaymentAllocation allocation = LoanCalculator.allocateRepayment(
                request.getAmount(), unpaidFeeAmounts, loan.getOutstandingBalance());

        // --- persistence: apply fee settlements ---
        BigDecimal remainingFeePayment = allocation.feesSettled();
        for (LoanFee fee : unpaidFees) {
            if (remainingFeePayment.compareTo(BigDecimal.ZERO) <= 0) break;
            if (remainingFeePayment.compareTo(fee.getAmount()) >= 0) {
                remainingFeePayment = remainingFeePayment.subtract(fee.getAmount());
                fee.setPaid(true);
                fee.setPaidDate(LocalDate.now());
                loanFeeRepository.save(fee);
            }
        }

        // --- persistence: apply principal settlement ---
        loan.setOutstandingBalance(allocation.remainingBalance());
        applyPrincipalToInstallments(loan, allocation.principalSettled());

        // --- persistence: close loan if fully paid ---
        if (allocation.remainingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            closeLoan(loan);
        }

        LocalDate paymentDate = request.getPaymentDate() != null
                ? request.getPaymentDate()
                : LocalDate.now();

        Repayment repayment = Repayment.builder()
                .repaymentReference(LoanCalculator.generateRepaymentReference())
                .loan(loan)
                .amount(request.getAmount())
                .principalPaid(allocation.principalSettled())
                .feesPaid(allocation.feesSettled())
                .paymentDate(paymentDate)
                .notes(request.getNotes())
                .build();

        Loan savedLoan = loanRepository.save(loan);
        Repayment saved = repaymentRepository.save(repayment);

        notificationService.sendNotification(
                savedLoan.getCustomer(), savedLoan, NotificationEventType.LOAN_REPAYMENT);
        log.info("Repayment {} of {} made on loan {}",
                saved.getRepaymentReference(), request.getAmount(), savedLoan.getLoanReference());

        return mapRepaymentToResponse(saved);
    }

    // =========================================================================
    // Loan state changes
    // =========================================================================

    @Override
    @Transactional
    public LoanResponse cancelLoan(Long id) {
        Loan loan = findById(id);
        if (loan.getStatus() != LoanStatus.OPEN) {
            throw new BusinessException("Only OPEN loans can be cancelled");
        }
        loan.setStatus(LoanStatus.CANCELLED);

        Customer customer = loan.getCustomer();
        creditLimitRepository.findByCustomer(customer).ifPresent(limit -> {
            limit.restoreLimit(loan.getPrincipalAmount());
            creditLimitRepository.save(limit);
        });
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
        notificationService.sendNotification(
                loan.getCustomer(), loan, NotificationEventType.LOAN_WRITTEN_OFF);
        return mapToResponse(loan);
    }

    // =========================================================================
    // Scheduled jobs
    // =========================================================================

    @Override
    @Transactional
    public void processOverdueLoans() {
        LocalDate today = LocalDate.now();
        List<Loan> openOverdue = loanRepository.findOpenLoansOverdue(today);
        log.info("Sweep: found {} open loans past due", openOverdue.size());

        for (Loan loan : openOverdue) {
            loan.setStatus(LoanStatus.OVERDUE);

            long daysOverdue = today.toEpochDay() - loan.getDueDate().toEpochDay();

            // --- pure predicate: isLateFeeApplicable ---
            loan.getProduct().getFees().stream()
                    .filter(f -> LoanCalculator.isLateFeeApplicable(f, daysOverdue))
                    .filter(f -> !loanFeeRepository.existsByLoanIdAndFeeTypeAndPaidFalse(
                            loan.getId(), FeeType.LATE_FEE))
                    .forEach(f -> {
                        // --- pure calculation ---
                        BigDecimal feeAmount = LoanCalculator.calculateFeeAmount(
                                f, loan.getOutstandingBalance());

                        // --- persistence ---
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
                        notificationService.sendNotification(
                                loan.getCustomer(), loan, NotificationEventType.LATE_FEE_APPLIED);
                        log.info("Applied late fee {} to loan {}", feeAmount, loan.getLoanReference());
                    });

            loanRepository.save(loan);
            notificationService.sendNotification(
                    loan.getCustomer(), loan, NotificationEventType.LOAN_OVERDUE);
        }
    }

    @Override
    @Transactional
    public void applyDailyFees() {
        LocalDate today = LocalDate.now();
        loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == LoanStatus.OPEN
                        || l.getStatus() == LoanStatus.OVERDUE)
                .forEach(loan -> {
                    loan.getProduct().getFees().stream()
                            .filter(f -> f.getFeeType() == FeeType.DAILY_FEE
                                    && Boolean.TRUE.equals(f.getActive()))
                            .forEach(f -> {
                                // --- pure calculation ---
                                BigDecimal dailyFeeAmount = LoanCalculator.calculateFeeAmount(
                                        f, loan.getOutstandingBalance());

                                // --- persistence ---
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
                                loan.setOutstandingBalance(
                                        loan.getOutstandingBalance().add(dailyFeeAmount));
                                loanRepository.save(loan);
                            });
                });
    }

    // =========================================================================
    // Private persistence helpers (imperative; never compute amounts themselves)
    // =========================================================================

    /** Persists one {@link LoanFee} row per active service fee on the product. */
    private void persistServiceFees(Loan loan, LoanProduct product) {
        product.getFees().stream()
                .filter(f -> f.getFeeType() == FeeType.SERVICE_FEE
                        && Boolean.TRUE.equals(f.getActive()))
                .forEach(f -> {
                    BigDecimal feeAmount = LoanCalculator.calculateFeeAmount(
                            f, loan.getPrincipalAmount());
                    loanFeeRepository.save(LoanFee.builder()
                            .loan(loan)
                            .productFee(f)
                            .feeType(FeeType.SERVICE_FEE)
                            .amount(feeAmount)
                            .appliedDate(loan.getDisbursementDate())
                            .paid(false)
                            .description(f.getDescription() != null ? f.getDescription() : "Service fee")
                            .build());
                });
    }

    /** Persists the flat interest {@link LoanFee} row. */
    private void persistInterestFee(Loan loan, LoanProduct product) {
        BigDecimal interestAmount = LoanCalculator.calculateInterestAmount(
                loan.getPrincipalAmount(), product);

        loanFeeRepository.save(LoanFee.builder()
                .loan(loan)
                .feeType(FeeType.INTEREST_FEE)
                .amount(interestAmount)
                .appliedDate(loan.getDisbursementDate())
                .paid(false)
                .description("Flat interest charge")
                .build());
    }

    /** Converts a {@link LoanCalculator} schedule into persisted installment rows. */
    private void persistInstallments(Loan loan, LoanProduct product) {
        List<LoanCalculator.InstallmentScheduleEntry> schedule = LoanCalculator.generateInstallmentSchedule(loan.getDisbursementDate(), loan.getOutstandingBalance(), product);

        for (LoanCalculator.InstallmentScheduleEntry entry : schedule) {
            LoanInstallment installment = LoanInstallment.builder()
                    .loan(loan)
                    .installmentNumber(entry.installmentNumber())
                    .principalAmount(entry.principalAmount())
                    .outstandingAmount(entry.principalAmount())
                    .dueDate(entry.dueDate())
                    .status(LoanStatus.OPEN)
                    .build();
            loan.getInstallments().add(installment);
        }
    }

    /** Walks open installments and deducts the settled principal from each in order. */
    private void applyPrincipalToInstallments(Loan loan, BigDecimal principalSettled) {
        BigDecimal remaining = principalSettled;
        List<LoanInstallment> installments = installmentRepository
                .findByLoanAndStatusOrderByDueDateAsc(loan, LoanStatus.OPEN);

        for (LoanInstallment installment : installments) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal outstanding = installment.getOutstandingAmount();
            BigDecimal applied = remaining.min(outstanding);

            installment.setOutstandingAmount(outstanding.subtract(applied));

            if (installment.getOutstandingAmount().compareTo(BigDecimal.ZERO) == 0) {
                installment.setStatus(LoanStatus.CLOSED);
                installment.setPaidDate(LocalDate.now());
            }
            installmentRepository.save(installment);
            remaining = remaining.subtract(applied);
        }
    }

    /** Marks the loan CLOSED and restores the customer's credit limit. */
    private void closeLoan(Loan loan) {
        loan.setStatus(LoanStatus.CLOSED);
        loan.setClosedDate(LocalDate.now());

        Customer customer = loan.getCustomer();
//        creditLimitRepository.findByCustomer(customer).ifPresent(limit -> {
//            limit.restoreLimit(loan.getPrincipalAmount());
//            creditLimitRepository.save(limit);
//        });
//        customerRepository.save(customer);
        notificationService.sendNotification(customer, loan, NotificationEventType.LOAN_CLOSED);
    }

    // =========================================================================
    // Finders
    // =========================================================================

    private Loan findById(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
    }

    private Loan findByIdAndCustomer(Long loanId, Long customerId) {
        return loanRepository.findByIdAndCustomerId(loanId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
    }

    // =========================================================================
    // Mapping
    // =========================================================================

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