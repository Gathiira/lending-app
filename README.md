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
- **Other:** Lombok, Spring Actuator

### 📦 [Key Components](Impl.md)

### 🗄️ Database

Flyway-managed migrations with versioned schema (V1-V5):
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

