package com.local.lms;

import com.local.lms.domain.entity.*;
import com.local.lms.domain.enums.*;
import com.local.lms.dto.request.CreateLoanRequest;
import com.local.lms.dto.request.RepaymentRequest;
import com.local.lms.dto.response.LoanResponse;
import com.local.lms.dto.response.RepaymentResponse;
import com.local.lms.exceptions.BusinessException;
import com.local.lms.repository.*;
import com.local.lms.service.NotificationService;
import com.local.lms.service.impl.LoanServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    // ----------------------------
    // CREATE LOAN - SUCCESS
    // ----------------------------
    @Test
    void createLoan_success() {

        Customer customer = Customer.builder()
                .id(1L)
                .active(true)
                .email("test@mail.com")
                .currentLoanLimit(new BigDecimal("10000"))
                .build();

        LoanProduct product = LoanProduct.builder()
                .id(1L)
                .active(true)
                .minAmount(new BigDecimal("1000"))
                .maxAmount(new BigDecimal("5000"))
                .loanType(LoanType.LUMP_SUM)
                .billingCycleType(BillingCycleType.INDIVIDUAL)
                .tenureType(TenureType.DAYS)
                .tenureValue(30)
                .fees(new ArrayList<>())
                .build();

        CreateLoanRequest request = new CreateLoanRequest();
        request.setCustomerId(1L);
        request.setProductId(1L);
        request.setAmount(new BigDecimal("2000"));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoanResponse response = loanService.createLoan(request);

        assertThat(response).isNotNull();
        assertThat(response.getPrincipalAmount()).isEqualByComparingTo("2000");

        verify(customerRepository).save(customer);
        verify(loanRepository, atLeast(1)).save(any(Loan.class));
        verify(notificationService).sendNotification(eq(customer), any(), any());
    }

    // ----------------------------
    // CREATE LOAN - INACTIVE PRODUCT
    // ----------------------------
    @Test
    void createLoan_shouldFail_whenProductInactive() {

        Customer customer = Customer.builder()
                .id(1L)
                .active(true)
                .currentLoanLimit(new BigDecimal("10000"))
                .build();

        LoanProduct product = LoanProduct.builder()
                .id(1L)
                .active(false)
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        CreateLoanRequest request = new CreateLoanRequest();
        request.setCustomerId(1L);
        request.setProductId(1L);
        request.setAmount(new BigDecimal("2000"));

        assertThatThrownBy(() -> loanService.createLoan(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Loan product is not active");
    }

    // ----------------------------
    // CREATE LOAN - LIMIT EXCEEDED
    // ----------------------------
    @Test
    void createLoan_shouldFail_whenLimitExceeded() {

        Customer customer = Customer.builder()
                .id(1L)
                .active(true)
                .currentLoanLimit(new BigDecimal("1000"))
                .build();

        LoanProduct product = LoanProduct.builder()
                .id(1L)
                .active(true)
                .minAmount(new BigDecimal("500"))
                .maxAmount(new BigDecimal("5000"))
                .loanType(LoanType.LUMP_SUM)
                .billingCycleType(BillingCycleType.INDIVIDUAL)
                .tenureType(TenureType.DAYS)
                .tenureValue(30)
                .fees(new ArrayList<>())
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        CreateLoanRequest request = new CreateLoanRequest();
        request.setCustomerId(1L);
        request.setProductId(1L);
        request.setAmount(new BigDecimal("2000"));

        assertThatThrownBy(() -> loanService.createLoan(request))
                .isInstanceOf(BusinessException.class);
    }

    // ----------------------------
    // REPAYMENT - INVALID AMOUNT
    // ----------------------------
    @Test
    void makeRepayment_shouldFail_whenAmountZero() {

        Loan loan = Loan.builder()
                .id(1L)
                .status(LoanStatus.OPEN)
                .build();

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        RepaymentRequest request = new RepaymentRequest();
        request.setLoanId(1L);
        request.setAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> loanService.makeRepayment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("positive");
    }

    // ----------------------------
    // REPAYMENT - SUCCESS FLOW
    // ----------------------------
    @Test
    void makeRepayment_success() {

        Loan loan = Loan.builder()
                .id(1L)
                .status(LoanStatus.OPEN)
                .outstandingBalance(new BigDecimal("2000"))
                .customer(Customer.builder()
                        .id(1L)
                        .currentLoanLimit(new BigDecimal("0"))
                        .build())
                .build();

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanFeeRepository.findByLoanIdAndPaidFalse(1L))
                .thenReturn(List.of());

        when(loanRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repaymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RepaymentRequest request = new RepaymentRequest();
        request.setLoanId(1L);
        request.setAmount(new BigDecimal("1000"));

        RepaymentResponse response = loanService.makeRepayment(request);

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo("1000");

        verify(repaymentRepository).save(any(Repayment.class));
        verify(notificationService).sendNotification(any(), any(), any());
    }

    // ----------------------------
    // CANCEL LOAN
    // ----------------------------
    @Test
    void cancelLoan_success() {

        Customer customer = Customer.builder()
                .id(1L)
                .currentLoanLimit(new BigDecimal("1000"))
                .build();

        LoanProduct product = LoanProduct.builder()
                .id(1L)
                .active(true)
                .minAmount(new BigDecimal("500"))
                .maxAmount(new BigDecimal("5000"))
                .loanType(LoanType.LUMP_SUM)
                .billingCycleType(BillingCycleType.INDIVIDUAL)
                .tenureType(TenureType.DAYS)
                .tenureValue(30)
                .fees(new ArrayList<>())
                .build();

        Loan loan = Loan.builder()
                .id(1L)
                .status(LoanStatus.OPEN)
                .principalAmount(new BigDecimal("500"))
                .customer(customer)
                .product(product)
                .build();

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        loanService.cancelLoan(1L);

        assertThat(customer.getCurrentLoanLimit())
                .isEqualByComparingTo("1500");

        verify(customerRepository).save(customer);
        verify(notificationService).sendNotification(eq(customer), any(), any());
    }

    // ----------------------------
    // WRITE OFF - INVALID STATE
    // ----------------------------
    @Test
    void writeOffLoan_shouldFail_whenNotOverdue() {

        Loan loan = Loan.builder()
                .id(1L)
                .status(LoanStatus.OPEN)
                .build();

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.writeOffLoan(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OVERDUE");
    }

    // ----------------------------
    // GET LOAN
    // ----------------------------
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
