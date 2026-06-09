package com.local.lms.service.impl;

import com.local.lms.domain.entity.Customer;
import com.local.lms.dto.request.LoginRequest;
import com.local.lms.dto.response.AuthResponse;
import com.local.lms.domain.entity.UserAccount;
import com.local.lms.domain.enums.AccountStatus;
import com.local.lms.dto.response.CustomerResponse;
import com.local.lms.exceptions.BusinessException;
import com.local.lms.repository.UserAccountRepository;
import com.local.lms.security.JwtUtil;
import com.local.lms.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CustomerServiceImpl customerService;

    @Override
    public AuthResponse login(LoginRequest request) {

        UserAccount user = userAccountRepository.findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new BusinessException("Invalid username or password"));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Account is not active");
        }

        // TODO disable this for production
//        user.setPassword(passwordEncoder.encode(request.getPassword()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("Invalid username or password");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userAccountRepository.save(user);

        String token = jwtUtil.generateAccessToken(user);
        AuthResponse authResponse =  AuthResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .token(token)
                .status(user.getStatus())
                .role(user.getRole())
                .lastLoginAt(user.getLastLoginAt())
                .build();

        Customer customer = user.getCustomer();
        if (customer != null) {
            CustomerResponse customerResponse = customerService.mapToResponse(customer);
            authResponse.setCustomer(customerResponse);
        }
        return authResponse;
    }
}