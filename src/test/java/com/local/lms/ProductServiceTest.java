package com.local.lms;

import com.local.lms.domain.entity.LoanProduct;
import com.local.lms.domain.enums.*;
import com.local.lms.dto.request.CreateLoanProductRequest;
import com.local.lms.dto.request.ProductFeeRequest;
import com.local.lms.dto.response.ProductResponse;
import com.local.lms.exceptions.BusinessException;
import com.local.lms.repository.LoanProductRepository;
import com.local.lms.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Tests")
class ProductServiceTest {

    @Mock
    private LoanProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private CreateLoanProductRequest validRequest;

    @BeforeEach
    void setUp() {

        ProductFeeRequest productFeeRequest = new ProductFeeRequest();
        productFeeRequest.setAmount(new BigDecimal("100"));
        productFeeRequest.setDescription("Test Description");
        productFeeRequest.setFeeType(FeeType.LATE_FEE);
        productFeeRequest.setCalculationMethod(FeeCalculationMethod.FIXED);


        validRequest = new CreateLoanProductRequest();
        validRequest.setName("Test Product");
        validRequest.setDescription("A test product");
        validRequest.setMinAmount(new BigDecimal("1000"));
        validRequest.setMaxAmount(new BigDecimal("50000"));
        validRequest.setTenureValue(30);
        validRequest.setTenureType(TenureType.DAYS);
        validRequest.setLoanType(LoanType.LUMP_SUM);
        validRequest.setBillingCycleType(BillingCycleType.INDIVIDUAL);
        validRequest.setGracePeriodDays(3);
        validRequest.setFees(List.of(productFeeRequest));
    }

    @Test
    @DisplayName("Should create product successfully")
    void createProduct_success() {
        when(productRepository.findByName("Test Product")).thenReturn(Optional.empty());

        LoanProduct savedProduct = LoanProduct.builder()
                .id(1L).name("Test Product").minAmount(new BigDecimal("1000"))
                .maxAmount(new BigDecimal("50000")).tenureValue(30)
                .interestRate(BigDecimal.valueOf(5))
                .tenureType(TenureType.DAYS).loanType(LoanType.LUMP_SUM)
                .billingCycleType(BillingCycleType.INDIVIDUAL).gracePeriodDays(3)
                .active(true).build();

        when(productRepository.save(any(LoanProduct.class))).thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Product");
        assertThat(response.getActive()).isTrue();
        verify(productRepository, times(1)).save(any(LoanProduct.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when product name already exists")
    void createProduct_duplicateName_throwsException() {
        when(productRepository.findByName("Test Product"))
                .thenReturn(Optional.of(new LoanProduct()));

        assertThatThrownBy(() -> productService.createProduct(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessException when minAmount > maxAmount")
    void createProduct_invalidAmountRange_throwsException() {
        validRequest.setMinAmount(new BigDecimal("60000"));
        validRequest.setMaxAmount(new BigDecimal("50000"));
        when(productRepository.findByName(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Min amount");
    }

    @Test
    @DisplayName("Should throw BusinessException when installment count missing for INSTALLMENT type")
    void createProduct_installmentMissingCount_throwsException() {
        validRequest.setLoanType(LoanType.INSTALLMENT);
        validRequest.setInstallmentCount(null);
        when(productRepository.findByName(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Installment count");
    }

    @Test
    @DisplayName("Should deactivate product")
    void deactivateProduct_success() {
        LoanProduct product = LoanProduct.builder().id(1L).name("Test").active(true).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        productService.deactivateProduct(1L);

        assertThat(product.getActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Should get active products only")
    void getActiveProducts_returnsOnlyActive() {
        LoanProduct p1 = LoanProduct.builder().id(1L).name("Active").active(true)
                .minAmount(BigDecimal.ONE).maxAmount(BigDecimal.TEN)
                .tenureValue(30).tenureType(TenureType.DAYS)
                .interestRate(BigDecimal.valueOf(5))
                .loanType(LoanType.LUMP_SUM).billingCycleType(BillingCycleType.INDIVIDUAL)
                .gracePeriodDays(0).build();

        when(productRepository.findByActiveTrue()).thenReturn(List.of(p1));

        List<ProductResponse> result = productService.getActiveProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Active");
    }
}
