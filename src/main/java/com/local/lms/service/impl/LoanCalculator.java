package com.local.lms.service.impl;

import com.local.lms.domain.entity.LoanProduct;
import com.local.lms.domain.entity.ProductFee;
import com.local.lms.domain.enums.FeeCalculationMethod;
import com.local.lms.domain.enums.FeeType;
import com.local.lms.domain.enums.TenureType;
import com.local.lms.exceptions.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pure calculation layer for loan domain logic.
 *
 * <p>All methods are static and side-effect-free: no I/O, no persistence,
 * no Spring context. This makes every rule independently unit-testable
 * without needing to wire a Spring application context or mock any repository.
 *
 * <p>The imperative layer ({@link LoanServiceImpl}) delegates every
 * financial computation here, retaining only persistence and notification
 * concerns.
 */
public final class LoanCalculator {

    private LoanCalculator() {
        // utility class – not instantiable
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    /**
     * Validates that {@code amount} falls within the product's min/max range.
     *
     * @throws BusinessException if the amount is out of range
     */
    public static void validateLoanAmount(BigDecimal amount, LoanProduct product) {
        if (amount.compareTo(product.getMinAmount()) < 0) {
            throw new BusinessException("Amount is below the product minimum of " + product.getMinAmount());
        }
        if (amount.compareTo(product.getMaxAmount()) > 0) {
            throw new BusinessException("Amount exceeds product maximum of " + product.getMaxAmount());
        }
    }

    // -------------------------------------------------------------------------
    // Date arithmetic
    // -------------------------------------------------------------------------

    /**
     * Returns the loan due date based on the product's tenure configuration.
     *
     * @param from    the disbursement date
     * @param product the loan product defining tenure type and value
     * @return the calculated due date
     */
    public static LocalDate calculateDueDate(LocalDate from, LoanProduct product) {
        return switch (product.getTenureType()) {
            case DAYS   -> from.plusDays(product.getTenureValue());
            case MONTHS -> from.plusMonths(product.getTenureValue());
        };
    }

    // -------------------------------------------------------------------------
    // Fee amounts
    // -------------------------------------------------------------------------

    /**
     * Calculates the monetary amount for a single {@link ProductFee}.
     *
     * <p>FIXED fees return the fee's own {@code amount} field directly.
     * PERCENTAGE fees return {@code principal × rate / 100}, rounded up
     * to the nearest whole unit (CEILING) so the lender never under-collects.
     *
     * @param fee       the product fee configuration
     * @param principal the base amount to apply a percentage fee against
     * @return the resolved fee amount, always ≥ 0
     */
    public static BigDecimal calculateFeeAmount(ProductFee fee, BigDecimal principal) {
        if (fee.getCalculationMethod() == FeeCalculationMethod.FIXED) {
            return fee.getAmount();
        }
        return principal
                .multiply(fee.getAmount())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING);
    }

    /**
     * Calculates the flat interest charge for the full loan tenure.
     *
     * <p>Formula: {@code principal × (annualRate / 100) × (months / 12)}.
     * For DAY-based tenures the month count is approximated as
     * {@code tenureDays / 30}. The result is rounded up (CEILING) so the
     * lender never under-collects fractional currency units.
     *
     * @param principal   the loan's principal amount
     * @param product     the loan product (provides rate and tenure)
     * @return the total interest amount to charge at origination
     */
    public static BigDecimal calculateInterestAmount(BigDecimal principal, LoanProduct product) {
        BigDecimal rate = product.getInterestRate()
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

        int months = product.getTenureType() == TenureType.MONTHS
                ? product.getTenureValue()
                : product.getTenureValue() / 30;

        return principal
                .multiply(rate)
                .multiply(BigDecimal.valueOf(months / 12.0))
                .setScale(0, RoundingMode.CEILING);
    }

    /**
     * Calculates the total service-fee amount by summing every active
     * {@link FeeType#SERVICE_FEE} entry on the product.
     *
     * @param principal the base amount used for percentage-based service fees
     * @param product   the loan product
     * @return total service fees, or {@link BigDecimal#ZERO} if none apply
     */
    public static BigDecimal calculateTotalServiceFees(BigDecimal principal, LoanProduct product) {
        return product.getFees().stream()
                .filter(f -> f.getFeeType() == FeeType.SERVICE_FEE && Boolean.TRUE.equals(f.getActive()))
                .map(f -> calculateFeeAmount(f, principal))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Returns the total amount that will be outstanding immediately after
     * disbursement: principal + service fees + interest.
     *
     * <p>This is the single source of truth for the opening outstanding
     * balance used when creating a loan.
     *
     * @param principal the loan's principal amount
     * @param product   the loan product
     * @return the opening outstanding balance
     */
    public static BigDecimal calculateOpeningOutstandingBalance(BigDecimal principal, LoanProduct product) {
        BigDecimal serviceFees = calculateTotalServiceFees(principal, product);
        BigDecimal interest    = calculateInterestAmount(principal, product);
        return principal.add(serviceFees).add(interest);
    }

    // -------------------------------------------------------------------------
    // Installment schedule
    // -------------------------------------------------------------------------

    /**
     * Result record for a generated installment schedule entry.
     *
     * <p>Intentionally free of JPA annotations so it can be created and
     * inspected in plain unit tests.
     */
    public record InstallmentScheduleEntry(
            int      installmentNumber,
            BigDecimal principalAmount,
            LocalDate  dueDate
    ) {}

    /**
     * Generates the full installment schedule for an INSTALLMENT-type loan.
     *
     * <p>The outstanding balance (including fees) is divided into
     * {@code product.installmentCount} equal parts. All installments except
     * the last use CEILING rounding so each regular payment is a round number.
     * The final installment is computed as {@code outstandingBalance - sum of
     * previous installments}, which absorbs any rounding remainder and guarantees
     * that the schedule sums exactly to the outstanding balance.
     *
     * @param disbursementDate   the date the loan was disbursed
     * @param outstandingBalance the total balance (principal + fees) to amortise
     * @param product            the loan product
     * @return ordered list of installment schedule entries (index 0 = first installment)
     */
    public static List<InstallmentScheduleEntry> generateInstallmentSchedule(
            LocalDate  disbursementDate,
            BigDecimal outstandingBalance,
            LoanProduct product
    ) {
        int count = product.getInstallmentCount();
        BigDecimal regularAmount = outstandingBalance
                .divide(BigDecimal.valueOf(count), 0, RoundingMode.CEILING);

        List<InstallmentScheduleEntry> schedule = new ArrayList<>(count);
        BigDecimal allocated = BigDecimal.ZERO;

        for (int i = 1; i <= count; i++) {
            LocalDate dueDate = switch (product.getTenureType()) {
                case MONTHS -> disbursementDate.plusMonths(i);
                case DAYS   -> disbursementDate.plusDays(
                        (long) product.getTenureValue() / count * i);
            };

            boolean isLast = (i == count);
            BigDecimal amount = isLast
                    ? outstandingBalance.subtract(allocated)  // absorbs rounding remainder
                    : regularAmount;

            schedule.add(new InstallmentScheduleEntry(i, amount, dueDate));
            allocated = allocated.add(amount);
        }
        return schedule;
    }

    // -------------------------------------------------------------------------
    // Repayment allocation
    // -------------------------------------------------------------------------

    /**
     * Result of allocating a payment against fees and principal.
     *
     * @param feesSettled      the portion of the payment absorbed by fees
     * @param principalSettled the portion of the payment applied to principal
     * @param remainingBalance the outstanding balance after this payment
     */
    public record RepaymentAllocation(
            BigDecimal feesSettled,
            BigDecimal principalSettled,
            BigDecimal remainingBalance
    ) {}

    /**
     * Allocates a payment amount: fees first, then principal.
     *
     * <p>This is a pure calculation: it does not mutate any entity; callers
     * are responsible for applying the result to their domain objects.
     *
     * @param paymentAmount      the payment to allocate
     * @param unpaidFeeAmounts   ordered list of unpaid fee amounts (fees-first order)
     * @param currentBalance     the current outstanding principal balance
     * @return the allocation result
     */
    public static RepaymentAllocation allocateRepayment(
            BigDecimal paymentAmount,
            List<BigDecimal> unpaidFeeAmounts,
            BigDecimal currentBalance
    ) {
        BigDecimal remaining  = paymentAmount;
        BigDecimal feesSettled = BigDecimal.ZERO;

        for (BigDecimal feeAmount : unpaidFeeAmounts) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            if (remaining.compareTo(feeAmount) >= 0) {
                remaining   = remaining.subtract(feeAmount);
                feesSettled = feesSettled.add(feeAmount);
            }
        }

        BigDecimal principalSettled = remaining.min(currentBalance);
        BigDecimal newBalance       = currentBalance.subtract(principalSettled);

        return new RepaymentAllocation(feesSettled, principalSettled, newBalance);
    }

    // -------------------------------------------------------------------------
    // Late / overdue logic
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if this fee configuration should trigger a late-fee
     * charge given the number of days the loan is overdue.
     *
     * @param fee        the product fee (must be {@link FeeType#LATE_FEE})
     * @param daysOverdue how many days past the due date the loan currently is
     */
    public static boolean isLateFeeApplicable(ProductFee fee, long daysOverdue) {
        return fee.getFeeType() == FeeType.LATE_FEE
                && Boolean.TRUE.equals(fee.getActive())
                && daysOverdue >= fee.getDaysAfterDue();
    }

    // -------------------------------------------------------------------------
    // Reference generation
    // -------------------------------------------------------------------------

    /**
     * Generates a unique loan reference in the form {@code LN-XXXXXXXX}.
     */
    public static String generateLoanReference() {
        return "LN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Generates a unique repayment reference in the form {@code RPY-XXXXXXXX}.
     */
    public static String generateRepaymentReference() {
        return "RPY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}