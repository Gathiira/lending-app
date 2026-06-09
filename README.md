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