# ✅ Lms Checklist

## 🧱 Setup

### Project Structure

* [X] Decide architecture style
* [X] Define modules/services:
    * [X] customer-service
    * [X] product-service
    * [x] loan-service
    * [x] scoring-engine
    * [x] notification-service
    * [x] common (DTOs, enums, utils)

### Spring Boot Setup

* [x] Initialize Spring Boot projects
* [x] Add dependencies:
    * [x] Spring Web
    * [x] Spring Data JPA
    * [x] Validation
    * [x] Lombok
    * [x] Spring Boot Test
    * [x] Flyway
* [x] Configure profiles (local/test/docker)

### Database

* [x] Choose DB (PostgreSQL)
* [x] Configure datasource
* [x] Enable auto schema creation and migrations (Flyway)
* [x] Add baseline migration setup

---

## 📦 Domain Modeling (Core Design Work)

### Key Enums

* [x] LoanStatus:

    * OPEN, CLOSED, CANCELLED, OVERDUE, WRITTEN_OFF
* [x] TenureType:

    * DAYS, MONTHS
* [x] FeeType:

    * SERVICE_FEE, DAILY_FEE, LATE_FEE
* [x] LoanStructure:

    * LUMP_SUM, INSTALLMENT
* [x] BillingCycleType:

    * INDIVIDUAL, CONSOLIDATED

---

## 🏦 Setup

* [x] Create all entities
* [x] Create all repository interfaces
* [x] Create service layer with business logic
* [x] Create controllers with REST endpoints

---

## ⏰ Sweep Jobs (Background Processing)

### Scheduled Jobs

* [x] Implement @Scheduled job for overdue detection
* [x] Identify overdue loans:

    * [x] due date passed
    * [x] unpaid balance exists

### Actions in Sweep Job

* [x] Mark loans OVERDUE
* [x] Apply late fees
* [x] Trigger notifications
* [x] Update interest/daily fees (if applicable)

---

## 🧪 Testing Strategy

### Unit Tests

* [x] Product service tests
* [x] Loan calculation logic tests
* [x] Repayment logic tests
* [x] Fee application tests

### Integration Tests

* [ ] Loan lifecycle flow
* [ ] API endpoint tests

### Edge Cases

* [x] Partial payments
* [x] Overpayments
* [x] Late fee triggers
* [x] Loan cancellation scenarios

## 10. 🌐 REST API Best Practices

* [x] Use proper HTTP methods
* [x] Consistent response format
* [x] Pagination for list endpoints
* [x] Proper status codes
* [x] Validation (@Validation)
* [x] Global exception handler
* [x] API versioning (e.g., /api/v1/loans)
* [ ] Idempotent endpoints for updates (PUT vs PATCH)
* [ ] introduce ReentrantLock for concurrency control

### API Docs

* [ ] Swagger/OpenAPI integration
* [ ] Document all endpoints

---