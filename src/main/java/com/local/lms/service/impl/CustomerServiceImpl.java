package com.local.lms.service.impl;

import com.local.lms.domain.entity.Customer;
import com.local.lms.dto.request.CreateCustomerRequest;
import com.local.lms.dto.request.UpdateCustomerLimitRequest;
import com.local.lms.dto.response.CustomerResponse;
import com.local.lms.exceptions.BusinessException;
import com.local.lms.exceptions.ResourceNotFoundException;
import com.local.lms.repository.CustomerRepository;
import com.local.lms.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Customer with email '" + request.getEmail() + "' already exists");
        }
        if (request.getNationalId() != null && customerRepository.existsByNationalId(request.getNationalId())) {
            throw new BusinessException("Customer with national ID '" + request.getNationalId() + "' already exists");
        }

        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .nationalId(request.getNationalId())
                .creditScore(request.getCreditScore() != null ? request.getCreditScore() : 0)
                .maxLoanLimit(request.getMaxLoanLimit())
                .currentLoanLimit(request.getMaxLoanLimit())
                .preferredChannel(request.getPreferredChannel())
                .active(true)
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Created customer: {} (id={})", saved.getEmail(), saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long id) {
        return mapToResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Long id, CreateCustomerRequest request) {
        Customer customer = findById(id);

        if (!customer.getEmail().equals(request.getEmail()) && customerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email '" + request.getEmail() + "' is already in use");
        }

        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setPreferredChannel(request.getPreferredChannel());

        return mapToResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public CustomerResponse updateLoanLimit(Long id, UpdateCustomerLimitRequest request) {
        Customer customer = findById(id);
        log.info("Updating loan limit for customer {} from {} to {}", id,
                customer.getMaxLoanLimit(), request.getMaxLoanLimit());
        customer.setMaxLoanLimit(request.getMaxLoanLimit());
        customer.setCurrentLoanLimit(request.getMaxLoanLimit());
        return mapToResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public void deactivateCustomer(Long id) {
        Customer customer = findById(id);
        customer.setActive(false);
        customerRepository.save(customer);
        log.info("Deactivated customer id={}", id);
    }

    @Override
    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    // ---- helpers ----
    public CustomerResponse mapToResponse(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .firstName(c.getFirstName())
                .lastName(c.getLastName())
                .email(c.getEmail())
                .phoneNumber(c.getPhoneNumber())
                .nationalId(c.getNationalId())
                .creditScore(c.getCreditScore())
                .maxLoanLimit(c.getMaxLoanLimit())
                .currentLoanLimit(c.getCurrentLoanLimit())
                .preferredChannel(c.getPreferredChannel())
                .active(c.getActive())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
