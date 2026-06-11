package com.local.lms;

import com.local.lms.core.EntityLockManager;
import com.local.lms.domain.entity.*;
import com.local.lms.domain.enums.*;
import com.local.lms.dto.request.ApplyLoanRequest;
import com.local.lms.dto.request.RepaymentRequest;
import com.local.lms.dto.response.LoanResponse;
import com.local.lms.dto.response.RepaymentResponse;
import com.local.lms.exceptions.BusinessException;
import com.local.lms.exceptions.ResourceNotFoundException;
import com.local.lms.repository.*;
import com.local.lms.service.NotificationService;
import com.local.lms.service.impl.LoanServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceImplTest {

    @InjectMocks
    private LoanServiceImpl loanService;

    @Mock private LoanRepository loanRepository;
    @Mock private LoanFeeRepository loanFeeRepository;
    @Mock private RepaymentRepository repaymentRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private LoanProductRepository productRepository;
    @Mock private NotificationService notificationService;
    @Mock private LoanInstallmentRepository installmentRepository;
    @Mock private CreditLimitRepository creditLimitRepository;
    @Mock private EntityLockManager lockManager;

    // -----------------------------------------------------------------------
    // SETUP — make lockManager transparent for all tests.
    // Without this, executeWithLock() returns null and the Supplier never runs.
    // -----------------------------------------------------------------------
    @BeforeEach
    void stubLockManager() {
        // Supplier variant (used by repayment and loan creation)
        lenient().when(lockManager.executeWithLock(anyString(), any(Supplier.class), anyLong()))
                .thenAnswer(invocation -> {
                    Supplier<?> action = invocation.getArgument(1);
                    return action.get();
                });

        // Supplier variant without timeout (if you have that overload)
        lenient().when(lockManager.executeWithLock(anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> action = invocation.getArgument(1);
                    return action.get();
                });
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private Customer activeCustomer() {
        return Customer.builder()
                .id(1L)
                .active(true)
                .email("test@mail.com")
                .currentLoanLimit(new BigDecimal("10000"))
                .build();
    }

    private LoanProduct activeLumpSumProduct() {
        return LoanProduct.builder()
                .id(1L)
                .active(true)
                .minAmount(new BigDecimal("1000"))
                .maxAmount(new BigDecimal("5000"))
                .interestRate(new BigDecimal("10.0"))
                .loanType(LoanType.LUMP_SUM)
                .billingCycleType(BillingCycleType.INDIVIDUAL)
                .tenureType(TenureType.MONTHS)
                .tenureValue(1)
                .fees(new ArrayList<>())
                .build();
    }

    private CreditLimit creditLimit(Customer customer, LoanProduct product, String available) {
        return CreditLimit.builder()
                .id(1L)
                .customer(customer)
                .loanProduct(product)
                .availableLimit(new BigDecimal(available))
                .frozenLimit(BigDecimal.ZERO)
                .currentLimit(new BigDecimal(available))
                .build();
    }

    private Loan openLoan(Customer customer, BigDecimal outstanding) {
        return Loan.builder()
                .id(1L)
                .status(LoanStatus.OPEN)
                .loanReference("LN-123")
                .principalAmount(new BigDecimal("2000"))
                .outstandingBalance(outstanding)
                .customer(customer)
                .fees(new ArrayList<>())
                .installments(new ArrayList<>())
                .build();
    }

    // -----------------------------------------------------------------------
    // CREATE LOAN — success
    // -----------------------------------------------------------------------
    @Test
    void createLoan_success() {
        Customer customer = activeCustomer();
        LoanProduct product = activeLumpSumProduct();
        CreditLimit limit   = creditLimit(customer, product, "10000");

        ApplyLoanRequest request = new ApplyLoanRequest();
        request.setCustomerId(1L);
        request.setAmount(new BigDecimal("2000"));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(creditLimitRepository.findByCustomer(customer)).thenReturn(Optional.of(limit));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanFeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoanResponse response = loanService.applyLoan(request);

        assertThat(response).isNotNull();
        assertThat(response.getPrincipalAmount()).isEqualByComparingTo("2000");
        verify(loanRepository, atLeastOnce()).save(any(Loan.class));
        verify(loanFeeRepository, atLeastOnce()).save(argThat(fee ->
                fee.getFeeType() == FeeType.INTEREST_FEE));
        verify(notificationService).sendNotification(eq(customer), any(),
                eq(NotificationEventType.LOAN_CREATED));
    }

    // -----------------------------------------------------------------------
    // CREATE LOAN — lock key is correct
    // -----------------------------------------------------------------------
    @Test
    void createLoan_shouldAcquireLockPerCustomer() {
        Customer customer = activeCustomer();
        LoanProduct product = activeLumpSumProduct();
        CreditLimit limit   = creditLimit(customer, product, "10000");

        ApplyLoanRequest request = new ApplyLoanRequest();
        request.setCustomerId(1L);
        request.setAmount(new BigDecimal("2000"));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(creditLimitRepository.findByCustomer(customer)).thenReturn(Optional.of(limit));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanFeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loanService.applyLoan(request);

        // assert the correct lock key pattern was used
        verify(lockManager).executeWithLock(
                eq("loan:customer:1"),
                any(Supplier.class),
                anyLong()
        );
    }

    // -----------------------------------------------------------------------
    // CREATE LOAN — inactive product
    // -----------------------------------------------------------------------
    @Test
    void createLoan_shouldFail_whenProductInactive() {
        Customer customer = activeCustomer();
        LoanProduct product = activeLumpSumProduct();
        product.setActive(false);
        CreditLimit limit   = creditLimit(customer, product, "10000");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(creditLimitRepository.findByCustomer(customer)).thenReturn(Optional.of(limit));

        ApplyLoanRequest request = new ApplyLoanRequest();
        request.setCustomerId(1L);
        request.setAmount(new BigDecimal("2000"));

        assertThatThrownBy(() -> loanService.applyLoan(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Loan product is not active");
    }

    // -----------------------------------------------------------------------
    // CREATE LOAN — credit limit exceeded
    // -----------------------------------------------------------------------
    @Test
    void createLoan_shouldFail_whenLimitExceeded() {
        Customer customer = activeCustomer();
        customer.setCurrentLoanLimit(new BigDecimal("1000"));
        CreditLimit limit = creditLimit(customer, null, "1000");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(creditLimitRepository.findByCustomer(customer)).thenReturn(Optional.of(limit));

        ApplyLoanRequest request = new ApplyLoanRequest();
        request.setCustomerId(1L);
        request.setAmount(new BigDecimal("2000")); // exceeds limit

        assertThatThrownBy(() -> loanService.applyLoan(request))
                .isInstanceOf(BusinessException.class);
    }

    // -----------------------------------------------------------------------
    // REPAYMENT — amount zero/negative rejected before lock
    // -----------------------------------------------------------------------
    @Test
    void makeRepayment_shouldFail_whenAmountZero() {
        RepaymentRequest request = new RepaymentRequest();
        request.setLoanId(1L);
        request.setCustomerId(1L);
        request.setAmount(BigDecimal.ZERO);

        // loan not found → ResourceNotFoundException before lock is ever reached
        assertThatThrownBy(() -> loanService.makeRepayment(request))
                .isInstanceOf(ResourceNotFoundException.class);

        // lock must never be acquired for invalid pre-conditions
        verifyNoInteractions(lockManager);
    }

    // -----------------------------------------------------------------------
    // REPAYMENT — success
    // -----------------------------------------------------------------------
    @Test
    void makeRepayment_success() {
        Customer customer = Customer.builder().id(1L).build();
        Loan loan = openLoan(customer, new BigDecimal("2200"));

        LoanFee interestFee = LoanFee.builder()
                .feeType(FeeType.INTEREST_FEE)
                .amount(new BigDecimal("200"))
                .paid(false)
                .build();

        when(loanRepository.findByIdAndCustomerId(1L, 1L)).thenReturn(Optional.of(loan));
        when(loanFeeRepository.findByLoanIdAndPaidFalse(1L)).thenReturn(List.of(interestFee));
        when(loanRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repaymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RepaymentRequest request = new RepaymentRequest();
        request.setLoanId(1L);
        request.setCustomerId(1L);
        request.setAmount(new BigDecimal("1000"));

        RepaymentResponse response = loanService.makeRepayment(request);

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo("1000");
        verify(repaymentRepository).save(any(Repayment.class));
        verify(notificationService).sendNotification(any(), any(), any());
    }

    // -----------------------------------------------------------------------
    // REPAYMENT — amount exceeds outstanding balance (validated inside lock)
    // -----------------------------------------------------------------------
    @Test
    void makeRepayment_shouldFail_whenAmountExceedsBalance() {
        Customer customer = Customer.builder().id(1L).build();
        Loan loan = openLoan(customer, new BigDecimal("500"));

        when(loanRepository.findByIdAndCustomerId(1L, 1L)).thenReturn(Optional.of(loan));

        RepaymentRequest request = new RepaymentRequest();
        request.setLoanId(1L);
        request.setCustomerId(1L);
        request.setAmount(new BigDecimal("1000")); // exceeds 500

        assertThatThrownBy(() -> loanService.makeRepayment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("outstanding balance");
    }

    // -----------------------------------------------------------------------
    // REPAYMENT — closed loan rejected inside lock (stale-state guard)
    // -----------------------------------------------------------------------
    @Test
    void makeRepayment_shouldFail_whenLoanNotActive() {
        Customer customer = Customer.builder().id(1L).build();
        Loan loan = openLoan(customer, new BigDecimal("2000"));
        loan.setStatus(LoanStatus.CLOSED);

        when(loanRepository.findByIdAndCustomerId(1L, 1L)).thenReturn(Optional.of(loan));

        RepaymentRequest request = new RepaymentRequest();
        request.setLoanId(1L);
        request.setCustomerId(1L);
        request.setAmount(new BigDecimal("500"));

        assertThatThrownBy(() -> loanService.makeRepayment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CLOSED");
    }

    // -----------------------------------------------------------------------
    // CANCEL LOAN — success + credit limit restored
    // -----------------------------------------------------------------------
    @Test
    void cancelLoan_success() {
        Customer customer = activeCustomer();
        LoanProduct product = activeLumpSumProduct();
        CreditLimit limit   = creditLimit(customer, product, "1500");

        ApplyLoanRequest request = new ApplyLoanRequest();
        request.setCustomerId(1L);
        request.setAmount(new BigDecimal("1500"));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(creditLimitRepository.findByCustomer(customer)).thenReturn(Optional.of(limit));
        when(loanRepository.save(any())).thenAnswer(inv -> {
            Loan l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });
        when(loanFeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoanResponse loanResponse = loanService.applyLoan(request);
        assertThat(loanResponse.getPrincipalAmount()).isEqualByComparingTo("1500");
        assertThat(limit.getAvailableLimit()).isEqualByComparingTo("0");

        // now cancel
        Loan loan = Loan.builder()
                .id(1L)
                .customer(customer)
                .product(product)
                .principalAmount(new BigDecimal("1500"))
                .status(LoanStatus.OPEN)
                .build();

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        loanService.cancelLoan(1L);

        assertThat(limit.getAvailableLimit()).isEqualByComparingTo("1500");
        verify(customerRepository).save(customer);
        verify(notificationService).sendNotification(eq(customer), any(),
                eq(NotificationEventType.LOAN_CREATED));
        verify(notificationService).sendNotification(eq(customer), any(),
                eq(NotificationEventType.LOAN_CANCELLED));
    }

    // -----------------------------------------------------------------------
    // WRITE OFF — rejected when not overdue
    // -----------------------------------------------------------------------
    @Test
    void writeOffLoan_shouldFail_whenNotOverdue() {
        Loan loan = Loan.builder().id(1L).status(LoanStatus.OPEN).build();
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.writeOffLoan(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OVERDUE");
    }

    // -----------------------------------------------------------------------
    // GET LOAN — success
    // -----------------------------------------------------------------------
    @Test
    void getLoan_success() {
        Loan loan = Loan.builder()
                .id(1L)
                .loanReference("LN-123")
                .customer(Customer.builder().id(1L).firstName("John").build())
                .product(LoanProduct.builder().id(1L).name("Prod").build())
                .installments(new ArrayList<>())
                .fees(new ArrayList<>())
                .build();

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        LoanResponse response = loanService.getLoan(1L);

        assertThat(response).isNotNull();
        assertThat(response.getLoanReference()).isEqualTo("LN-123");
    }
}