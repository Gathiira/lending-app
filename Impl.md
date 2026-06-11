
# Key Services: Scoring & Loan
## Overview
The LMS system contains two primary service domains: **Credit Scoring & Worthiness** and **Loan Management**. These services work together to evaluate customer creditworthiness, manage loan lifecycle, and handle repayments.


## 1. Credit Scoring Service

### Purpose
Evaluates customer creditworthiness using a composite scoring model. A credit score determines the customer's risk band and suggested credit limit for loan eligibility.

### Architecture
**Location:** `/src/main/java/com/local/lms/scoring/` and `/src/main/java/com/local/lms/service/impl/CreditWorthinessServiceImpl.java`

The system uses a **strategy pattern** with four independent evaluators that each contribute points to a total score:

#### [CreditScoreEvaluator](src/main/java/com/local/lms/scoring/CreditScoreEvaluator.java) (Interface)
```java
public interface CreditScoreEvaluator {
    String name();
    int maxScore();
    int evaluate(Long customerId);
}
```

Each evaluator is a Spring component that scores a single credit dimension.

### Scoring Components

#### 1. [LoanHistoryEvaluator](src/main/java/com/local/lms/scoring/impl/LoanHistoryEvaluator.java) (0-300 points)
Evaluates past loan behavior based on default history.
- **No history:** 150 points (neutral)
- **0% default rate:** 300 points
- **1-5% defaults:** 250 points
- **6-10% defaults:** 180 points
- **11-20% defaults:** 100 points
- **>20% defaults:** 0 points

#### 2. [RepaymentHistoryEvaluator](src/main/java/com/local/lms/scoring/impl/RepaymentHistoryEvaluator.java) (0-400 points)
Scores based on timeliness of repayments.
- **No history:** 200 points (neutral)
- **≥95% on-time:** 400 points
- **85-94% on-time:** 320 points
- **70-84% on-time:** 220 points
- **50-69% on-time:** 120 points
- **<50% on-time:** 40 points

#### 3. [CreditUtilizationEvaluator](src/main/java/com/local/lms/scoring/impl/CreditUtilizationEvaluator.java) (0-200 points)
Measures how much available credit is being used.
- **No credit limit:** 100 points (neutral)
- **≤30% utilized:** 200 points (excellent)
- **31-50% utilized:** 160 points
- **51-70% utilized:** 100 points
- **71-90% utilized:** 50 points
- **>90% utilized:** 10 points

#### 4. [CrbProfileEvaluator](src/main/java/com/local/lms/scoring/impl/CrbProfileEvaluator.java) (0-100 points)
Integration placeholder for Credit Reference Bureau (CRB) data.
- Currently returns neutral score of 50 points
- **Status:** Pending integration with CRB providers (e.g., Metropol, TransUnion KE)

### [CreditWorthinessServiceImpl](src/main/java/com/local/lms/service/impl/CreditWorthinessServiceImpl.java)

#### Key Method
```java
CreditScoreResult evaluate(Long customerId, Long productId)
```

**Process:**
1. Independently evaluate each of the 4 scoring dimensions
2. Sum scores (max 1000 points)
3. Derive risk band based on total score
4. Calculate utilization factor based on current credit limit usage
5. Compute suggested credit limit using score multiplier and utilization factor

#### Risk Band Classification
| Total Score | Risk Band | Credit Limit Multiplier |
|-------------|-----------|------------------------|
| ≥800       | EXCELLENT | 100% of product max    |
| 600-799    | GOOD      | 75% of product max     |
| 400-599    | FAIR      | 50% of product max     |
| 200-399    | POOR      | 25% of product max     |
| <200       | VERY_POOR | 10% of product max     |

#### Utilization Factor (Adjustment)
Applied to customers with existing credit limits:
- **0-30% utilized:** 1.0× multiplier (no penalty)
- **31-50% utilized:** 0.9× multiplier
- **51-70% utilized:** 0.75× multiplier
- **>70% utilized:** 0.60× multiplier

#### Output: [CreditScoreResult](src/main/java/com/local/lms/dto/response/CreditScoreResult.java)
Contains:
- `totalScore`: Composite score (0-1000)
- `riskBand`: Customer risk classification
- Individual scores for each evaluator
- `creditLimit`: Suggested credit limit for the product
- `evaluatedAt`: Timestamp

---

## 2. Loan Service

### Purpose
Manages the complete loan lifecycle: creation, application, repayment, state changes, and scheduled maintenance tasks.

### Architecture
**Location:** `/src/main/java/com/local/lms/service/LoanService.java` and `/src/main/java/com/local/lms/service/impl/LoanServiceImpl.java`

**Design principle:** Imperative service owns transactions and persistence; all financial calculations are delegated to [LoanCalculator](src/main/java/com/local/lms/service/impl/LoanCalculator.java).

### Core Operations

#### 1. **Loan Creation**

##### [createLoan(CreateLoanRequest request)](src/main/java/com/local/lms/service/LoanService.java)
Admin creates a loan directly for a customer.
- Validates product and customer exist and are active
- Delegates calculations to `LoanCalculator`
- Persists loan + fees + installments (if applicable)
- Sends LOAN_CREATED notification

##### [applyLoan(ApplyLoanRequest request)](src/main/java/com/local/lms/service/LoanService.java)
Customer applies for a loan using their credit limit.
- Retrieves customer's credit limit (must exist)
- **Freezes** requested amount on credit limit
- Creates loan
- **Utilizes** frozen limit (converts from frozen → used state)

#### 2. **Query Operations**
- `getLoan(Long id)` - Fetch single loan
- `getLoan(Long id, Long customerId)` - Fetch loan with ownership check
- `getLoanByReference(String reference)` - Find by unique reference
- `getCustomerLoans(Long customerId)` - All loans for a customer
- `getPage(LoanSearchRequest, Pageable)` - Paginated search with filtering

#### 3. **Repayment**

##### [makeRepayment(RepaymentRequest request)](src/main/java/com/local/lms/service/LoanService.java)
Customer makes a payment against their loan.

**Allocation strategy (fees-first):**
1. Unpaid fees are settled first (e.g., service fees, interest, late fees)
2. Remaining amount is applied to principal
3. Principal is deducted from open installments in order
4. Loan automatically closes if balance reaches zero

**Validations:**
- Loan must be active (status = OPEN or OVERDUE)
- Payment amount > 0
- Payment amount ≤ outstanding balance

#### 4. **Loan State Changes**

##### [cancelLoan(Long id)](src/main/java/com/local/lms/service/LoanService.java)
- Only OPEN loans can be cancelled
- Restores principal to customer's credit limit
- Sends LOAN_CANCELLED notification

##### [writeOffLoan(Long id)](src/main/java/com/local/lms/service/LoanService.java)
- Only OVERDUE loans can be written off
- Records write-off date
- Sends LOAN_WRITTEN_OFF notification

#### 5. **Scheduled Operations** (Called by [LoanSweepScheduler](src/main/java/com/local/lms/scheduler/LoanSweepScheduler.java))

##### [processOverdueLoans()](src/main/java/com/local/lms/service/LoanService.java)
**Daily task** to identify and process overdue loans.
- Finds all OPEN loans past their due date
- Marks them as OVERDUE
- Applies late fees if configured and applicable
- Sends LOAN_OVERDUE and LATE_FEE_APPLIED notifications

Late fee eligibility: `fee.daysAfterDue ≤ actual daysOverdue`

##### [applyDailyFees()](src/main/java/com/local/lms/service/LoanService.java)
**Daily task** to accrue daily fees on active loans.
- Processes all OPEN and OVERDUE loans
- Applies each configured DAILY_FEE
- Increases outstanding balance by fee amount

---

## 3. LoanCalculator (Pure Calculation Layer)

### Purpose
**Static, side-effect-free** utility for all financial calculations. Designed to be unit-tested independently without Spring context.

**Location:** [`LoanCalculator.java`](src/main/java/com/local/lms/service/impl/LoanCalculator.java)

### Key Methods

#### Amount Validation
```java
validateLoanAmount(BigDecimal amount, LoanProduct product)
// Ensures amount ∈ [product.minAmount, product.maxAmount]
```

#### Date Calculations
```java
calculateDueDate(LocalDate disbursementDate, LoanProduct product)
// Returns disbursementDate + tenure (MONTHS or DAYS)
```

#### Fee Calculations
```java
calculateFeeAmount(ProductFee fee, BigDecimal principal)
// FIXED: returns fee.amount
// PERCENTAGE: returns principal × (fee.amount / 100), rounded CEILING
```

```java
calculateInterestAmount(BigDecimal principal, LoanProduct product)
// Formula: principal × (rate / 100) × (months / 12)
// Rounded CEILING to ensure no under-collection
```

```java
calculateTotalServiceFees(BigDecimal principal, LoanProduct product)
// Sum of all active SERVICE_FEE entries
```

```java
calculateOpeningOutstandingBalance(BigDecimal principal, LoanProduct product)
// Returns: principal + serviceFees + interest
// Single source of truth for outstanding balance at creation
```

#### Installment Schedule
```java
generateInstallmentSchedule(LocalDate disbursementDate, BigDecimal outstandingBalance, LoanProduct product)
// Generates list of InstallmentScheduleEntry records
// Divides balance equally with CEILING rounding
// Final installment absorbs any rounding remainder
```

**Record:**
```java
record InstallmentScheduleEntry(
    int installmentNumber,
    BigDecimal principalAmount,
    LocalDate dueDate
)
```

#### Repayment Allocation
```java
allocateRepayment(BigDecimal paymentAmount, List<BigDecimal> unpaidFeeAmounts, BigDecimal currentBalance)
// Returns RepaymentAllocation record with:
//   - feesSettled: amount applied to fees
//   - principalSettled: amount applied to principal
//   - remainingBalance: balance after payment
```

#### Late Fee Logic
```java
isLateFeeApplicable(ProductFee fee, long daysOverdue)
// Returns true if:
//   - fee.feeType == LATE_FEE
//   - fee.active == true
//   - daysOverdue >= fee.daysAfterDue
```

---

## Data Flow Example

### Applying for a Loan

```
1. Customer calls applyLoan(amount, notes)
   ↓
2. LoanServiceImpl retrieves customer and credit limit
   ↓
3. creditLimit.freeze(amount)        [Freezes requested amount]
   ↓
4. LoanCalculator.validateLoanAmount(amount, product)
   ↓
5. LoanCalculator.calculateDueDate(today, product)
   ↓
6. LoanCalculator.calculateOpeningOutstandingBalance(amount, product)
   ├─ calculateTotalServiceFees(amount, product)
   └─ calculateInterestAmount(amount, product)
   ↓
7. Create Loan entity with calculated values
   ↓
8. Persist service fees as LoanFee records
   ↓
9. Persist interest fee as LoanFee record
   ↓
10. If INSTALLMENT loan: LoanCalculator.generateInstallmentSchedule(...)
    Then persist each as LoanInstallment record
    ↓
11. creditLimit.utilizeFrozenLimit(amount)  [Converts to used]
    ↓
12. Send LOAN_CREATED notification
    ↓
13. Return LoanResponse
```

### Making a Repayment

```
1. Customer calls makeRepayment(amount)
   ↓
2. Retrieve loan and validate (active, amount > 0, amount ≤ balance)
   ↓
3. Query unpaid fees
   ↓
4. LoanCalculator.allocateRepayment(amount, unpaidFees, currentBalance)
   ├─ Fees settled amount
   ├─ Principal settled amount
   └─ New remaining balance
   ↓
5. Mark applicable fees as paid
   ↓
6. applyPrincipalToInstallments(loan, principalSettled)
   [Deducts from open installments in order]
   ↓
7. Create Repayment record with allocation breakdown
   ↓
8. If remainingBalance == 0: closeLoan(loan)
   ↓
9. Send LOAN_REPAYMENT notification
   ↓
10. Return RepaymentResponse
```

---

## Integration Points

### Credit Scoring → Loan Service
- **Credit check** before loan approval
- **Risk-based pricing** using evaluated risk band
- **Credit limit management** based on score-derived suggestion

### Notification Service
Both services integrate with NotificationService to send events:
- `LOAN_CREATED`, `LOAN_CANCELLED`, `LOAN_CLOSED`, `LOAN_OVERDUE`, `LOAN_REPAYMENT`
- `LATE_FEE_APPLIED`, `LOAN_WRITTEN_OFF`

### Repository Layer
- [LoanRepository](src/main/java/com/local/lms/repository/LoanRepository.java) - Loan queries
- [LoanFeeRepository](src/main/java/com/local/lms/repository/LoanFeeRepository.java) - Fee persistence
- [LoanInstallmentRepository](src/main/java/com/local/lms/repository/LoanInstallmentRepository.java) - Installment persistence
- [RepaymentRepository](src/main/java/com/local/lms/repository/RepaymentRepository.java) - Repayment tracking
- [CreditLimitRepository](src/main/java/com/local/lms/repository/CreditLimitRepository.java) - Credit limit queries

---

## Key Design Patterns

1. **Strategy Pattern** (Scoring): Multiple evaluators plugged into CreditWorthinessService
2. **Pure Functions** (LoanCalculator): Stateless, side-effect-free calculations for testability
3. **Separation of Concerns**: Service handles persistence/transactions; Calculator handles math
4. **Builder Pattern**: Domain entities and DTOs use fluent builders
5. **Transactional Boundaries**: All writes are `@Transactional` with explicit rollback on exceptions