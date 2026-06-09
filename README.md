# Loan Management System (LMS)

## 📋 Project Overview

**LMS** is a Spring Boot-based **Loan Management System** that handles the complete loan lifecycle from customer onboarding through repayment processing.

### 🛠️ Tech Stack
- **Framework:** Spring Boot 4.0.6 (Java 17)
- **Database:** PostgreSQL with Flyway migrations
- **ORM:** Spring Data JPA
- **Security:** Spring Security + JWT (JJWT)
- **Validation:** Spring Validation
- **API Documentation:** Swagger/OpenAPI
- **Testing:** JUnit, Spring Security Test, H2 database
- **Other:** Lombok, Spring Mail, Spring Actuator

### 🏗️ Architecture & Modules

The project follows a **layered architecture** with the following core modules:

1. **Customer Module** - Customer profiles and credit limit management
2. **Product Module** - Loan product definitions and fee configurations
3. **Loan Module** - Loan lifecycle management (creation, disbursement, repayment)
4. **Notification Module** - Event-driven notification system
5. **Security Module** - JWT-based authentication and authorization
6. **Scheduler Module** - Background jobs for loan sweep operations

### 📦 Key Components

**Domain Entities:**
- `Customer` - Customer profiles with credit limits
- `LoanProduct` - Loan product configurations
- `Loan` - Individual loan instances
- `LoanInstallment` - Installment tracking
- `CreditLimit` - Credit limit management
- `Notification` - Notification logs

**Controllers:**
- `/auth` - Authentication endpoints
- `/customers` - Customer management
- `/products` - Loan products
- `/loans` - Loan operations
- `/credits` - Credit limit management
- `/notifications` - Notification retrieval

### 🔄 Core Features
[Overview](lms.png)
- **Loan Creation & Disbursement** - Create loans and disburse funds
- **Repayment Processing** - Handle customer payments
- **Fee Management** - Service fees, daily fees, late fees
- **Installment Billing** - Individual and consolidated billing modes
- **Automated Sweep** - Scheduled job for overdue detection and late fee application
- **Event-Driven Notifications** - Notify customers of loan events
- **Security** - JWT authentication with role-based access control
- 


### 🗄️ Database

Flyway-managed migrations with versioned schema (V1-V4):
- Initial schema setup
- Seed data for products and customers
- Security account configuration
- User account data

### Running the Application using docker compose
1. Ensure Docker and Docker Compose are installed on your machine.
2. Clone the repository and navigate to the project directory.
3. Run the following command to start the application and PostgreSQL database:
    ```bash
      docker-compose up --build
    ```
4. The application will automatically run Flyway migrations to set up the database schema and seed initial data.
5. The application will be accessible at `http://localhost:8085`.
6. use the postman Api collection provided in the [lms postman collection](lms.postman_collection.json)  to test the API endpoints.

### logical flow of the application
1. **Customer Onboarding**: Create customer profiles and set credit limits.
  - the customer applies a credit limit request which is then approved by an admin. Once approved, the customer can apply for loans up to their credit limit.
2. **Loan Product Configuration**: Define loan products with fees and billing modes.
  - this is already seeded in the db
3. **Loan Creation**: Create loans for customers based on products and credit limits.
    - customer using /apply-loan, can apply loan using their credit limit. The system checks if the requested amount is within the customer's credit limit and if the loan product is valid before creating the loan.
4. **Loan Disbursement**: Disburse funds to customers.
5. **Repayment Processing**: Handle customer payments and update loan status.
6. **Automated Sweep**: Scheduled job to detect overdue loans and apply late fees.
7. **Event Notifications**: Notify customers of loan events (creation, disbursement, repayment, overdue).
8. **Security**: Authenticate users and authorize access to endpoints based on roles
