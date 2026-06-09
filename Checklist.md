# ✅ Lms Checklist

## 1. 🧱 Setup

### Project Structure

* [X] Decide architecture style
* [X] Define modules/services:
    * [X] customer-service
    * [X] product-service
    * [ ] loan-service
    * [ ] notification-service
    * [ ] common (DTOs, enums, utils)

### Spring Boot Setup

* [ ] Initialize Spring Boot projects
* [ ] Add dependencies:

    * [ ] Spring Web
    * [ ] Spring Data JPA
    * [ ] Validation
    * [ ] Lombok
    * [ ] Spring Boot Test
    * [ ] Flyway
* [ ] Configure profiles (local/test/prod)

### Database

* [ ] Choose DB (PostgreSQL)
* [ ] Configure datasource
* [ ] Enable auto schema creation OR migrations (Flyway)
* [ ] Add baseline migration setup

---

## 2. 📦 Domain Modeling (Core Design Work)

### Key Enums

* [ ] LoanStatus:

    * OPEN, CLOSED, CANCELLED, OVERDUE, WRITTEN_OFF
* [ ] TenureType:

    * DAYS, MONTHS
* [ ] FeeType:

    * SERVICE_FEE, DAILY_FEE, LATE_FEE
* [ ] LoanStructure:

    * LUMP_SUM, INSTALLMENT
* [ ] BillingCycleType:

    * INDIVIDUAL, CONSOLIDATED

---

## 3. 🏦 Product Module (Loan Product Engine)

### Product Entity

* [ ] Create `LoanProduct` entity
* [ ] Fields:

    * [ ] name
    * [ ] interest rate (optional if included)
    * [ ] tenure (value + type)
    * [ ] loan structure (lump sum / installment)

### Fee Configuration Model

* [ ] Create `ProductFee` entity
* [ ] Support:

    * [ ] SERVICE_FEE (fixed or %)
    * [ ] DAILY_FEE
    * [ ] LATE_FEE
* [ ] Configure:

    * [ ] amount or percentage
    * [ ] trigger rules
    * [ ] days after due

### Product APIs

* [ ] POST /products (create product)
* [ ] GET /products
* [ ] GET /products/{id}
* [ ] PUT /products/{id}

---

## 4. 👤 Customer Profile Module

### Customer Entity

* [ ] Create `Customer` entity
* [ ] Fields:

    * [ ] name
    * [ ] phone/email
    * [ ] national ID (optional)
    * [ ] credit score (optional)

### Loan Limit Management

* [ ] Create `CreditLimitRequest` entity
* [ ] Fields:

    * [ ] customerId
    * [ ] supporting docs
    * [ ] status
  
* [ ] Create `CreditLimit` entity
* [ ] Fields:

    * [ ] customerId
    * [ ] requestId
    * [ ] productId
    * [ ] currentLimit
    * [ ] frozenLimit
    * [ ] availableLimit

### APIs

* [ ] POST /customers
* [ ] GET /customers/{id}
* [ ] PATCH /customers/{id}/limitApply
* [ ] PATCH /customers/{id}/limitApprove
* [ ] GET /customers/{id}/loans

---

## 5. 💰 Loan Management Module

### Loan Entity

* [ ] Create `Loan` entity
* [ ] Fields:

    * [ ] customerId
    * [ ] productId
    * [ ] principal amount
    * [ ] status (LoanState)
    * [ ] disbursement date
    * [ ] due date
    * [ ] remaining balance
    * [ ] structure type

### Installments (if applicable)

* [ ] Create `LoanInstallment` entity
* [ ] Fields:

    * [ ] loanId
    * [ ] due date
    * [ ] amount
    * [ ] paid amount
    * [ ] status

### Loan Lifecycle Logic

* [ ] Loan creation flow
* [ ] Loan disbursement logic
* [ ] Repayment processing
* [ ] Balance recalculation

### Billing Logic

* [ ] Individual due date calculation
* [ ] Consolidated billing support
* [ ] Loan grouping by customer

### APIs

* [ ] POST /loans (create loan)
* [ ] POST /loans/{id}/disburse
* [ ] POST /loans/{id}/repay
* [ ] GET /loans/{id}
* [ ] GET /loans?status=

---

## 6. ⏰ Sweep Jobs (Background Processing)

### Scheduled Jobs

* [ ] Implement @Scheduled job for overdue detection
* [ ] Identify overdue loans:

    * [ ] due date passed
    * [ ] unpaid balance exists

### Actions in Sweep Job

* [ ] Mark loans OVERDUE
* [ ] Apply late fees
* [ ] Trigger notifications
* [ ] Update interest/daily fees (if applicable)

---

## 7. 🔔 Notification Module (Event-Driven System)

### Event System

* [ ] Define domain events:

    * LoanCreatedEvent
    * LoanDisbursedEvent
    * PaymentReceivedEvent
    * LoanOverdueEvent

### Event Handling

* [ ] Use Spring Events
* [ ] Listener for each event type

### Notification Entity

* [ ] Notification log table
* [ ] Fields:

    * [ ] customerId
    * [ ] channel (SMS/email)
    * [ ] message
    * [ ] status

### Templates

* [ ] Create template engine
* [ ] Store templates in DB:
    * [ ] loan creation
    * [ ] repayment reminder
    * [ ] overdue notice

### APIs

* [ ] GET /notifications/{customerId}
* [ ] POST /notifications/send (optional manual trigger)

---

## 8. 🧪 Testing Strategy

### Unit Tests

* [ ] Product service tests
* [ ] Loan calculation logic tests
* [ ] Repayment logic tests
* [ ] Fee application tests

### Integration Tests

* [ ] Loan lifecycle flow
* [ ] API endpoint tests

### Edge Cases

* [ ] Partial payments
* [ ] Overpayments
* [ ] Late fee triggers
* [ ] Loan cancellation scenarios

---

## 9. 📄 Database Design & Migrations

* [ ] Create ER diagram
* [ ] Add Flyway migrations:

    * [ ] V1__init_schema.sql
* [ ] Seed data:
    * [ ] sample products
    * [ ] customers
    * [ ] loans
    * [ ] repayment history
---

## 10. 🌐 REST API Best Practices

* [ ] Use proper HTTP methods
* [ ] Consistent response format
* [ ] Pagination for list endpoints
* [ ] Proper status codes
* [ ] Validation (@Validation)
* [ ] Global exception handler

---

## 11. 📚 Documentation

### README

* [ ] Project overview
* [ ] Architecture diagram
* [ ] Setup instructions
* [ ] Running application
* [ ] DB setup
* [ ] Seed data usage

### API Docs

* [ ] Swagger/OpenAPI integration
* [ ] Document all endpoints

---