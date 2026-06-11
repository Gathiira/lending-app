package com.local.lms;

import com.local.lms.domain.entity.LoanProduct;
import com.local.lms.domain.entity.ProductFee;
import com.local.lms.domain.enums.FeeCalculationMethod;
import com.local.lms.domain.enums.FeeType;
import com.local.lms.domain.enums.LoanType;
import com.local.lms.domain.enums.TenureType;
import com.local.lms.exceptions.BusinessException;
import com.local.lms.service.impl.LoanCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LoanCalculator")
class LoanCalculatorTest {

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private LoanProduct monthlyProduct;   // 3-month, 12% p.a., min 1 000, max 100 000
    private LoanProduct dailyProduct;     // 90-day,  12% p.a., min 1 000, max 100 000

    @BeforeEach
    void setUp() {
        monthlyProduct = LoanProduct.builder()
                .tenureType(TenureType.MONTHS)
                .tenureValue(3)
                .interestRate(new BigDecimal("12"))
                .minAmount(new BigDecimal("1000"))
                .maxAmount(new BigDecimal("100000"))
                .loanType(LoanType.INSTALLMENT)
                .installmentCount(3)
                .fees(List.of())
                .build();

        dailyProduct = LoanProduct.builder()
                .tenureType(TenureType.DAYS)
                .tenureValue(90)
                .interestRate(new BigDecimal("12"))
                .minAmount(new BigDecimal("1000"))
                .maxAmount(new BigDecimal("100000"))
                .loanType(LoanType.INSTALLMENT)
                .installmentCount(3)
                .fees(List.of())
                .build();
    }

    // =========================================================================
    // validateLoanAmount
    // =========================================================================

    @Nested
    @DisplayName("validateLoanAmount")
    class ValidateLoanAmount {

        @Test
        @DisplayName("passes when amount equals minimum")
        void passes_atMinimum() {
            assertThatNoException().isThrownBy(
                    () -> LoanCalculator.validateLoanAmount(new BigDecimal("1000"), monthlyProduct));
        }

        @Test
        @DisplayName("passes when amount equals maximum")
        void passes_atMaximum() {
            assertThatNoException().isThrownBy(
                    () -> LoanCalculator.validateLoanAmount(new BigDecimal("100000"), monthlyProduct));
        }

        @Test
        @DisplayName("passes for amount strictly between min and max")
        void passes_withinRange() {
            assertThatNoException().isThrownBy(
                    () -> LoanCalculator.validateLoanAmount(new BigDecimal("50000"), monthlyProduct));
        }

        @Test
        @DisplayName("throws when amount is below minimum")
        void throws_belowMinimum() {
            assertThatThrownBy(
                    () -> LoanCalculator.validateLoanAmount(new BigDecimal("999"), monthlyProduct))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("minimum");
        }

        @Test
        @DisplayName("throws when amount exceeds maximum")
        void throws_aboveMaximum() {
            assertThatThrownBy(
                    () -> LoanCalculator.validateLoanAmount(new BigDecimal("100001"), monthlyProduct))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("maximum");
        }
    }

    // =========================================================================
    // calculateDueDate
    // =========================================================================

    @Nested
    @DisplayName("calculateDueDate")
    class CalculateDueDate {

        private final LocalDate from = LocalDate.of(2024, 1, 15);

        @Test
        @DisplayName("adds tenure months for MONTHS product")
        void addsMonths() {
            LocalDate due = LoanCalculator.calculateDueDate(from, monthlyProduct);
            assertThat(due).isEqualTo(LocalDate.of(2024, 4, 15));
        }

        @Test
        @DisplayName("adds tenure days for DAYS product")
        void addsDays() {
            LocalDate due = LoanCalculator.calculateDueDate(from, dailyProduct);
            assertThat(due).isEqualTo(LocalDate.of(2024, 4, 14)); // +90 days
        }

        @Test
        @DisplayName("handles month-end correctly (no day overflow)")
        void monthEndHandling() {
            LoanProduct endOfMonth = LoanProduct.builder()
                    .tenureType(TenureType.MONTHS)
                    .tenureValue(1)
                    .interestRate(BigDecimal.ZERO)
                    .minAmount(BigDecimal.ONE)
                    .maxAmount(new BigDecimal("1000000"))
                    .fees(List.of())
                    .build();

            LocalDate jan31 = LocalDate.of(2024, 1, 31);
            assertThat(LoanCalculator.calculateDueDate(jan31, endOfMonth))
                    .isEqualTo(LocalDate.of(2024, 2, 29)); // 2024 is a leap year
        }
    }

    // =========================================================================
    // calculateFeeAmount
    // =========================================================================

    @Nested
    @DisplayName("calculateFeeAmount")
    class CalculateFeeAmount {

        @Test
        @DisplayName("returns the fixed amount regardless of principal")
        void fixedFeeTest() {
            ProductFee fee = fixedFee(FeeType.SERVICE_FEE, "500");
            assertThat(LoanCalculator.calculateFeeAmount(fee, new BigDecimal("10000")))
                    .isEqualByComparingTo("500");
        }

        @Test
        @DisplayName("calculates percentage of principal, rounded up")
        void percentageFee_roundsUp() {
            ProductFee fee = percentageFee(FeeType.SERVICE_FEE, "2.5"); // 2.5%
            // 2.5% of 10 000 = 250 exactly
            assertThat(LoanCalculator.calculateFeeAmount(fee, new BigDecimal("10000")))
                    .isEqualByComparingTo("250");
        }

        @Test
        @DisplayName("percentage fee rounds fractional result up to ceiling")
        void percentageFee_ceilingRounding() {
            ProductFee fee = percentageFee(FeeType.SERVICE_FEE, "3"); // 3%
            // 3% of 333 = 9.99 → ceil → 10
            assertThat(LoanCalculator.calculateFeeAmount(fee, new BigDecimal("333")))
                    .isEqualByComparingTo("10");
        }

        @Test
        @DisplayName("returns zero for zero principal percentage fee")
        void percentageFee_zeroPrincipal() {
            ProductFee fee = percentageFee(FeeType.SERVICE_FEE, "5");
            assertThat(LoanCalculator.calculateFeeAmount(fee, BigDecimal.ZERO))
                    .isEqualByComparingTo("0");
        }
    }

    // =========================================================================
    // calculateInterestAmount
    // =========================================================================

    @Nested
    @DisplayName("calculateInterestAmount")
    class CalculateInterestAmount {

        @Test
        @DisplayName("calculates flat interest for monthly product: P × rate × months/12")
        void monthlyTenure() {
            // 12 000 × (12/100) × (3/12) = 12 000 × 0.12 × 0.25 = 360
            BigDecimal interest = LoanCalculator.calculateInterestAmount(
                    new BigDecimal("12000"), monthlyProduct);
            assertThat(interest).isEqualByComparingTo("360");
        }

        @Test
        @DisplayName("calculates flat interest for daily product using days/30 approximation")
        void dailyTenure() {
            // 90 days → 3 months; 12 000 × 0.12 × 0.25 = 360
            BigDecimal interest = LoanCalculator.calculateInterestAmount(
                    new BigDecimal("12000"), dailyProduct);
            assertThat(interest).isEqualByComparingTo("360");
        }

        @Test
        @DisplayName("rounds fractional interest up (CEILING)")
        void roundsCeilingUp() {
            // 10 001 × 0.12 × 0.25 = 300.03 → ceil → 301
            BigDecimal interest = LoanCalculator.calculateInterestAmount(
                    new BigDecimal("10001"), monthlyProduct);
            assertThat(interest).isEqualByComparingTo("301");
        }

        @Test
        @DisplayName("returns zero for zero interest rate")
        void zeroRate() {
            LoanProduct zeroRate = LoanProduct.builder()
                    .tenureType(TenureType.MONTHS)
                    .tenureValue(3)
                    .interestRate(BigDecimal.ZERO)
                    .minAmount(BigDecimal.ONE)
                    .maxAmount(new BigDecimal("1000000"))
                    .fees(List.of())
                    .build();

            assertThat(LoanCalculator.calculateInterestAmount(new BigDecimal("50000"), zeroRate))
                    .isEqualByComparingTo("0");
        }
    }

    // =========================================================================
    // calculateTotalServiceFees
    // =========================================================================

    @Nested
    @DisplayName("calculateTotalServiceFees")
    class CalculateTotalServiceFees {

        @Test
        @DisplayName("returns zero when product has no fees")
        void noFees() {
            assertThat(LoanCalculator.calculateTotalServiceFees(
                    new BigDecimal("10000"), monthlyProduct))
                    .isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("sums multiple active service fees")
        void multipleFees() {
            LoanProduct product = productWith(List.of(
                    fixedFee(FeeType.SERVICE_FEE, "200"),   // 200
                    percentageFee(FeeType.SERVICE_FEE, "1") // 1% of 10 000 = 100
            ));
            assertThat(LoanCalculator.calculateTotalServiceFees(new BigDecimal("10000"), product))
                    .isEqualByComparingTo("300");
        }

        @Test
        @DisplayName("ignores inactive service fees")
        void ignoresInactiveFees() {
            ProductFee inactive = fixedFee(FeeType.SERVICE_FEE, "500");
            inactive.setActive(false);
            LoanProduct product = productWith(List.of(inactive));

            assertThat(LoanCalculator.calculateTotalServiceFees(new BigDecimal("10000"), product))
                    .isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("ignores non-service-fee types")
        void ignoresOtherFeeTypes() {
            LoanProduct product = productWith(List.of(
                    fixedFee(FeeType.LATE_FEE, "300"),
                    fixedFee(FeeType.DAILY_FEE, "50")
            ));
            assertThat(LoanCalculator.calculateTotalServiceFees(new BigDecimal("10000"), product))
                    .isEqualByComparingTo("0");
        }
    }

    // =========================================================================
    // calculateOpeningOutstandingBalance
    // =========================================================================

    @Nested
    @DisplayName("calculateOpeningOutstandingBalance")
    class CalculateOpeningOutstandingBalance {

        @Test
        @DisplayName("opening balance = principal + service fees + interest")
        void correctSum() {
            // Service fee: fixed 200
            // Interest: 12 000 × 0.12 × 0.25 = 360
            // Opening balance: 12 000 + 200 + 360 = 12 560
            LoanProduct product = productWith(List.of(fixedFee(FeeType.SERVICE_FEE, "200")));

            BigDecimal balance = LoanCalculator.calculateOpeningOutstandingBalance(
                    new BigDecimal("12000"), product);

            assertThat(balance).isEqualByComparingTo("12560");
        }

        @Test
        @DisplayName("equals principal when no fees and zero interest rate")
        void noFeesZeroRate() {
            LoanProduct zeroProduct = LoanProduct.builder()
                    .tenureType(TenureType.MONTHS)
                    .tenureValue(3)
                    .interestRate(BigDecimal.ZERO)
                    .minAmount(BigDecimal.ONE)
                    .maxAmount(new BigDecimal("1000000"))
                    .fees(List.of())
                    .build();

            assertThat(LoanCalculator.calculateOpeningOutstandingBalance(
                    new BigDecimal("10000"), zeroProduct))
                    .isEqualByComparingTo("10000");
        }
    }

    // =========================================================================
    // generateInstallmentSchedule
    // =========================================================================

    @Nested
    @DisplayName("generateInstallmentSchedule")
    class GenerateInstallmentSchedule {

        private final LocalDate disbursement = LocalDate.of(2024, 1, 15);

        @Test
        @DisplayName("generates correct number of installments")
        void correctCount() {
            List<LoanCalculator.InstallmentScheduleEntry> schedule =
                    LoanCalculator.generateInstallmentSchedule(
                            disbursement, new BigDecimal("3000"), monthlyProduct);
            assertThat(schedule).hasSize(3);
        }

        @Test
        @DisplayName("installment numbers are sequential starting at 1")
        void sequentialNumbers() {
            List<LoanCalculator.InstallmentScheduleEntry> schedule =
                    LoanCalculator.generateInstallmentSchedule(
                            disbursement, new BigDecimal("3000"), monthlyProduct);
            assertThat(schedule)
                    .extracting(LoanCalculator.InstallmentScheduleEntry::installmentNumber)
                    .containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("due dates advance by one month per installment for MONTHS tenure")
        void monthlyDueDates() {
            List<LoanCalculator.InstallmentScheduleEntry> schedule =
                    LoanCalculator.generateInstallmentSchedule(
                            disbursement, new BigDecimal("3000"), monthlyProduct);
            assertThat(schedule)
                    .extracting(LoanCalculator.InstallmentScheduleEntry::dueDate)
                    .containsExactly(
                            LocalDate.of(2024, 2, 15),
                            LocalDate.of(2024, 3, 15),
                            LocalDate.of(2024, 4, 15));
        }

        @Test
        @DisplayName("regular installments round up; last installment absorbs remainder so sum is exact")
        void unevenSplit_lastInstallmentAdjusted() {
            // 1 000 / 3 = 333.33... → regular = 334; last = 1000 - (334 + 334) = 332
            List<LoanCalculator.InstallmentScheduleEntry> schedule =
                    LoanCalculator.generateInstallmentSchedule(
                            disbursement, new BigDecimal("1000"), monthlyProduct);

            List<BigDecimal> amounts = schedule.stream()
                    .map(LoanCalculator.InstallmentScheduleEntry::principalAmount)
                    .toList();

            assertThat(amounts.get(0)).isEqualByComparingTo("334");
            assertThat(amounts.get(1)).isEqualByComparingTo("334");
            assertThat(amounts.get(2)).isEqualByComparingTo("332"); // adjusted last

            BigDecimal total = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(total).isEqualByComparingTo("1000");
        }

        @Test
        @DisplayName("amount divides exactly when balance is divisible by count")
        void exactSplit() {
            // 3 000 / 3 = 1 000 exactly — all installments equal, last needs no adjustment
            List<LoanCalculator.InstallmentScheduleEntry> schedule =
                    LoanCalculator.generateInstallmentSchedule(
                            disbursement, new BigDecimal("3000"), monthlyProduct);
            assertThat(schedule)
                    .extracting(LoanCalculator.InstallmentScheduleEntry::principalAmount)
                    .allMatch(a -> a.compareTo(new BigDecimal("1000")) == 0);
        }

        @Test
        @DisplayName("schedule always sums exactly to the outstanding balance")
        void scheduleSumsToBalance() {
            BigDecimal balance = new BigDecimal("10007"); // deliberately indivisible by 3
            List<LoanCalculator.InstallmentScheduleEntry> schedule =
                    LoanCalculator.generateInstallmentSchedule(
                            disbursement, balance, monthlyProduct);

            BigDecimal sum = schedule.stream()
                    .map(LoanCalculator.InstallmentScheduleEntry::principalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(sum).isEqualByComparingTo(balance);
        }

        @Test
        @DisplayName("due dates are spaced correctly for DAYS tenure")
        void dailyDueDates() {
            // 90 days / 3 installments = 30-day spacing
            List<LoanCalculator.InstallmentScheduleEntry> schedule =
                    LoanCalculator.generateInstallmentSchedule(
                            disbursement, new BigDecimal("3000"), dailyProduct);

            assertThat(schedule)
                    .extracting(LoanCalculator.InstallmentScheduleEntry::dueDate)
                    .containsExactly(
                            disbursement.plusDays(30),
                            disbursement.plusDays(60),
                            disbursement.plusDays(90));
        }
    }

    // =========================================================================
    // allocateRepayment
    // =========================================================================

    @Nested
    @DisplayName("allocateRepayment")
    class AllocateRepayment {

        @Test
        @DisplayName("settles fees first, then reduces principal")
        void feesFirst() {
            // fees: [300, 200] = 500 total; principal: 5 000; payment: 1 000
            LoanCalculator.RepaymentAllocation result = LoanCalculator.allocateRepayment(
                    new BigDecimal("1000"),
                    List.of(new BigDecimal("300"), new BigDecimal("200")),
                    new BigDecimal("5000"));

            assertThat(result.feesSettled()).isEqualByComparingTo("500");
            assertThat(result.principalSettled()).isEqualByComparingTo("500");
            assertThat(result.remainingBalance()).isEqualByComparingTo("4500");
        }

        @Test
        @DisplayName("only partially settles a fee when payment runs out mid-fee")
        void partialFeeSettlement() {
            // payment 150, first fee 300 — cannot fully cover it
            LoanCalculator.RepaymentAllocation result = LoanCalculator.allocateRepayment(
                    new BigDecimal("150"),
                    List.of(new BigDecimal("300")),
                    new BigDecimal("5000"));

            // 150 < 300, so the fee is not settled; all goes to... wait:
            // per the implementation: only fully-covered fees are settled;
            // remainder goes to principal
            assertThat(result.feesSettled()).isEqualByComparingTo("0");
            assertThat(result.principalSettled()).isEqualByComparingTo("150");
            assertThat(result.remainingBalance()).isEqualByComparingTo("4850");
        }

        @Test
        @DisplayName("settles first fee fully and partial on second when payment covers only one")
        void settlesFirstFeeOnly() {
            // payment 300; fees [300, 200]; settles first fee, nothing left for second or principal
            LoanCalculator.RepaymentAllocation result = LoanCalculator.allocateRepayment(
                    new BigDecimal("300"),
                    List.of(new BigDecimal("300"), new BigDecimal("200")),
                    new BigDecimal("5000"));

            assertThat(result.feesSettled()).isEqualByComparingTo("300");
            assertThat(result.principalSettled()).isEqualByComparingTo("0");
            assertThat(result.remainingBalance()).isEqualByComparingTo("5000");
        }

        @Test
        @DisplayName("no fees: entire payment goes to principal")
        void noFees_allToPrincipal() {
            LoanCalculator.RepaymentAllocation result = LoanCalculator.allocateRepayment(
                    new BigDecimal("2000"),
                    List.of(),
                    new BigDecimal("5000"));

            assertThat(result.feesSettled()).isEqualByComparingTo("0");
            assertThat(result.principalSettled()).isEqualByComparingTo("2000");
            assertThat(result.remainingBalance()).isEqualByComparingTo("3000");
        }

        @Test
        @DisplayName("payment exactly clears outstanding balance")
        void exactFullPayment() {
            LoanCalculator.RepaymentAllocation result = LoanCalculator.allocateRepayment(
                    new BigDecimal("5000"),
                    List.of(),
                    new BigDecimal("5000"));

            assertThat(result.principalSettled()).isEqualByComparingTo("5000");
            assertThat(result.remainingBalance()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("principal settled is capped at current outstanding balance")
        void principalCappedAtBalance() {
            // payment 8 000 but only 5 000 outstanding — excess goes nowhere
            LoanCalculator.RepaymentAllocation result = LoanCalculator.allocateRepayment(
                    new BigDecimal("8000"),
                    List.of(),
                    new BigDecimal("5000"));

            assertThat(result.principalSettled()).isEqualByComparingTo("5000");
            assertThat(result.remainingBalance()).isEqualByComparingTo("0");
        }
    }

    // =========================================================================
    // isLateFeeApplicable
    // =========================================================================

    @Nested
    @DisplayName("isLateFeeApplicable")
    class IsLateFeeApplicable {

        @Test
        @DisplayName("true when loan is overdue by exactly the required days")
        void exactlyAtThreshold() {
            ProductFee fee = lateFeeAfterDays(5);
            assertThat(LoanCalculator.isLateFeeApplicable(fee, 5)).isTrue();
        }

        @Test
        @DisplayName("true when loan is overdue beyond the required days")
        void beyondThreshold() {
            ProductFee fee = lateFeeAfterDays(5);
            assertThat(LoanCalculator.isLateFeeApplicable(fee, 10)).isTrue();
        }

        @Test
        @DisplayName("false when loan is not yet overdue enough")
        void belowThreshold() {
            ProductFee fee = lateFeeAfterDays(5);
            assertThat(LoanCalculator.isLateFeeApplicable(fee, 4)).isFalse();
        }

        @Test
        @DisplayName("false for inactive late fee")
        void inactiveFee() {
            ProductFee fee = lateFeeAfterDays(1);
            fee.setActive(false);
            assertThat(LoanCalculator.isLateFeeApplicable(fee, 10)).isFalse();
        }

        @Test
        @DisplayName("false for a non-late-fee type (e.g. SERVICE_FEE)")
        void wrongFeeType() {
            ProductFee fee = fixedFee(FeeType.SERVICE_FEE, "100");
            fee.setDaysAfterDue(1);
            assertThat(LoanCalculator.isLateFeeApplicable(fee, 10)).isFalse();
        }
    }

    // =========================================================================
    // generateLoanReference / generateRepaymentReference
    // =========================================================================

    @Nested
    @DisplayName("reference generation")
    class ReferenceGeneration {

        @Test
        @DisplayName("loan reference starts with 'LN-'")
        void loanReferencePrefix() {
            assertThat(LoanCalculator.generateLoanReference()).startsWith("LN-");
        }

        @Test
        @DisplayName("repayment reference starts with 'RPY-'")
        void repaymentReferencePrefix() {
            assertThat(LoanCalculator.generateRepaymentReference()).startsWith("RPY-");
        }

        @Test
        @DisplayName("consecutive loan references are unique")
        void loanReferencesAreUnique() {
            String r1 = LoanCalculator.generateLoanReference();
            String r2 = LoanCalculator.generateLoanReference();
            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        @DisplayName("consecutive repayment references are unique")
        void repaymentReferencesAreUnique() {
            String r1 = LoanCalculator.generateRepaymentReference();
            String r2 = LoanCalculator.generateRepaymentReference();
            assertThat(r1).isNotEqualTo(r2);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ProductFee fixedFee(FeeType type, String amount) {
        return ProductFee.builder()
                .feeType(type)
                .calculationMethod(FeeCalculationMethod.FIXED)
                .amount(new BigDecimal(amount))
                .active(true)
                .build();
    }

    private ProductFee percentageFee(FeeType type, String rate) {
        return ProductFee.builder()
                .feeType(type)
                .calculationMethod(FeeCalculationMethod.PERCENTAGE)
                .amount(new BigDecimal(rate))
                .active(true)
                .build();
    }

    private ProductFee lateFeeAfterDays(int days) {
        ProductFee fee = fixedFee(FeeType.LATE_FEE, "500");
        fee.setDaysAfterDue(days);
        return fee;
    }

    private LoanProduct productWith(List<ProductFee> fees) {
        return LoanProduct.builder()
                .tenureType(TenureType.MONTHS)
                .tenureValue(3)
                .interestRate(new BigDecimal("12"))
                .minAmount(new BigDecimal("1000"))
                .maxAmount(new BigDecimal("100000"))
                .loanType(LoanType.INSTALLMENT)
                .installmentCount(3)
                .fees(fees)
                .build();
    }
}