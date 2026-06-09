package com.local.lms;

import com.local.lms.domain.entity.Customer;
import com.local.lms.domain.entity.UserAccount;
import com.local.lms.domain.enums.AccountStatus;
import com.local.lms.dto.request.LoginRequest;
import com.local.lms.dto.response.AuthResponse;
import com.local.lms.dto.response.CustomerResponse;
import com.local.lms.exceptions.BusinessException;
import com.local.lms.repository.UserAccountRepository;
import com.local.lms.security.JwtUtil;
import com.local.lms.service.impl.AuthServiceImpl;
import com.local.lms.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomerServiceImpl customerService;

    // --------------------------
    // SUCCESS LOGIN
    // --------------------------
    @Test
    void login_success_shouldReturnToken() {

        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("pass123");

        UserAccount user = UserAccount.builder()
                .id(1L)
                .username("john")
                .password("hashed")
                .status(AccountStatus.ACTIVE)
                .build();

        when(userAccountRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("pass123", "hashed"))
                .thenReturn(true);

        when(jwtUtil.generateAccessToken(user))
                .thenReturn("jwt-token");

        Customer customer = Customer.builder()
                .id(10L)
                .build();

        user.setCustomer(customer);

        CustomerResponse customerResponse = CustomerResponse.builder()
                .id(10L)
                .build();

        when(customerService.mapToResponse(customer))
                .thenReturn(customerResponse);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("john");
        assertThat(response.getCustomer()).isNotNull();

        verify(userAccountRepository).save(any(UserAccount.class));
    }

    // --------------------------
    // USER NOT FOUND
    // --------------------------
    @Test
    void login_shouldFail_whenUserNotFound() {

        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("pass123");

        when(userAccountRepository.findByUsername("john"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid username or password");
    }

    // --------------------------
    // ACCOUNT NOT ACTIVE
    // --------------------------
    @Test
    void login_shouldFail_whenAccountInactive() {

        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("pass123");

        UserAccount user = UserAccount.builder()
                .id(1L)
                .username("john")
                .status(AccountStatus.INACTIVE)
                .password("hashed")
                .build();

        when(userAccountRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");
    }

    // --------------------------
    // INVALID PASSWORD
    // --------------------------
    @Test
    void login_shouldFail_whenPasswordInvalid() {

        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("wrong");

        UserAccount user = UserAccount.builder()
                .id(1L)
                .username("john")
                .status(AccountStatus.ACTIVE)
                .password("hashed")
                .build();

        when(userAccountRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("wrong", "hashed"))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid username or password");
    }

    // --------------------------
    // LOGIN WITHOUT CUSTOMER
    // --------------------------
    @Test
    void login_success_withoutCustomer() {

        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("pass123");

        UserAccount user = UserAccount.builder()
                .id(1L)
                .username("john")
                .status(AccountStatus.ACTIVE)
                .password("hashed")
                .build();

        when(userAccountRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("pass123", "hashed"))
                .thenReturn(true);

        when(jwtUtil.generateAccessToken(user))
                .thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getCustomer()).isNull();

        verify(customerService, never()).mapToResponse(any());
    }
}
