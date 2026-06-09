package com.local.lms;

import com.local.lms.domain.entity.Customer;
import com.local.lms.domain.enums.NotificationChannel;
import com.local.lms.dto.request.CreateCustomerRequest;
import com.local.lms.dto.request.UpdateCustomerLimitRequest;
import com.local.lms.dto.response.CustomerResponse;
import com.local.lms.exceptions.BusinessException;
import com.local.lms.repository.CustomerRepository;
import com.local.lms.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private CreateCustomerRequest request;

    @BeforeEach
    void setUp() {
        request = new CreateCustomerRequest();
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setEmail("jane@example.com");
        request.setPhoneNumber("+254700000000");
        request.setNationalId("11223344");
        request.setCreditScore(700);
        request.setMaxLoanLimit(new BigDecimal("100000"));
        request.setPreferredChannel(NotificationChannel.EMAIL);
    }

    @Test
    @DisplayName("Should create customer successfully")
    void createCustomer_success() {
        when(customerRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(customerRepository.existsByNationalId("11223344")).thenReturn(false);

        Customer saved = Customer.builder().id(1L).firstName("Jane").lastName("Doe")
                .email("jane@example.com").phoneNumber("+254700000000")
                .nationalId("11223344").creditScore(700)
                .maxLoanLimit(new BigDecimal("100000")).currentLoanLimit(new BigDecimal("100000"))
                .preferredChannel(NotificationChannel.EMAIL).active(true).build();
        when(customerRepository.save(any(Customer.class))).thenReturn(saved);

        CustomerResponse response = customerService.createCustomer(request);

        assertThat(response.getEmail()).isEqualTo("jane@example.com");
        assertThat(response.getMaxLoanLimit()).isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException for duplicate email")
    void createCustomer_duplicateEmail_throwsException() {
        when(customerRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update loan limit")
    void updateLoanLimit_success() {
        Customer customer = Customer.builder().id(1L).email("jane@example.com")
                .maxLoanLimit(new BigDecimal("100000")).currentLoanLimit(new BigDecimal("100000"))
                .preferredChannel(NotificationChannel.EMAIL).firstName("Jane").lastName("Doe")
                .active(true).build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);

        UpdateCustomerLimitRequest limitRequest = new UpdateCustomerLimitRequest();
        limitRequest.setMaxLoanLimit(new BigDecimal("200000"));

        CustomerResponse response = customerService.updateLoanLimit(1L, limitRequest);
        assertThat(customer.getMaxLoanLimit()).isEqualByComparingTo("200000");
    }

    @Test
    @DisplayName("Should deactivate customer")
    void deactivateCustomer_success() {
        Customer customer = Customer.builder().id(1L).active(true)
                .email("jane@example.com").firstName("Jane").lastName("Doe")
                .preferredChannel(NotificationChannel.EMAIL)
                .maxLoanLimit(BigDecimal.ZERO).currentLoanLimit(BigDecimal.ZERO).build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);

        customerService.deactivateCustomer(1L);
        assertThat(customer.getActive()).isFalse();
    }
}
