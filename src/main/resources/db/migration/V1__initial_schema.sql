-- =========================================================
-- V1__initial_schema.sql
-- Initial schema for the Lms Application
-- =========================================================

-- ENUMS
CREATE TYPE tenure_type AS ENUM ('DAYS', 'MONTHS');
CREATE TYPE fee_type AS ENUM ('SERVICE_FEE', 'DAILY_FEE', 'LATE_FEE', 'INTEREST_FEE');
CREATE TYPE fee_calculation_method AS ENUM ('FIXED', 'PERCENTAGE');
CREATE TYPE loan_type AS ENUM ('LUMP_SUM', 'INSTALLMENT');
CREATE TYPE loan_status AS ENUM ('OPEN', 'CLOSED', 'CANCELLED', 'OVERDUE', 'WRITTEN_OFF', 'PENDING_APPROVAL');
CREATE TYPE billing_cycle_type AS ENUM ('INDIVIDUAL', 'CONSOLIDATED');
CREATE TYPE notification_channel AS ENUM ('EMAIL', 'SMS');
CREATE TYPE notification_event_type AS ENUM (
    'LOAN_CREATED', 'LOAN_DISBURSED', 'LOAN_REPAYMENT', 'LOAN_OVERDUE',
    'LOAN_CLOSED', 'LOAN_CANCELLED', 'LOAN_WRITTEN_OFF',
    'DUE_DATE_REMINDER', 'LATE_FEE_APPLIED'
);
CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'FAILED');

-- =========================================================
-- CUSTOMERS
-- =========================================================
CREATE TABLE customers (
                           id                  BIGSERIAL PRIMARY KEY,
                           first_name          VARCHAR(100) NOT NULL,
                           last_name           VARCHAR(100) NOT NULL,
                           email               VARCHAR(255) NOT NULL UNIQUE,
                           phone_number        VARCHAR(20),
                           national_id         VARCHAR(50) UNIQUE,
                           credit_score        INTEGER DEFAULT 0,
                           max_loan_limit      NUMERIC(19,4) NOT NULL DEFAULT 0,
                           current_loan_limit  NUMERIC(19,4) NOT NULL DEFAULT 0,
                           preferred_channel   notification_channel NOT NULL DEFAULT 'EMAIL',
                           active              BOOLEAN NOT NULL DEFAULT TRUE,
                           created_by           VARCHAR(100),
                           updated_by           VARCHAR(100),
                           created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                           updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =========================================================
-- LOAN PRODUCTS
-- =========================================================
CREATE TABLE loan_products (
                               id                      BIGSERIAL PRIMARY KEY,
                               name                    VARCHAR(255) NOT NULL UNIQUE,
                               description             TEXT,
                               min_amount              NUMERIC(19,4) NOT NULL,
                               max_amount              NUMERIC(19,4) NOT NULL,
                               interest_rate              NUMERIC(5,2) NOT NULL,
                               tenure_value            INTEGER NOT NULL,
                               tenure_type             tenure_type NOT NULL,
                               loan_type               loan_type NOT NULL DEFAULT 'LUMP_SUM',
                               installment_count       INTEGER,
                               billing_cycle_type      billing_cycle_type NOT NULL DEFAULT 'INDIVIDUAL',
                               grace_period_days       INTEGER NOT NULL DEFAULT 0,
                               active                  BOOLEAN NOT NULL DEFAULT TRUE,
                               created_by           VARCHAR(100),
                               updated_by           VARCHAR(100),
                               created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                               updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =========================================================
-- PRODUCT FEES
-- =========================================================
CREATE TABLE product_fees (
                              id                      BIGSERIAL PRIMARY KEY,
                              product_id              BIGINT NOT NULL REFERENCES loan_products(id),
                              fee_type                fee_type NOT NULL,
                              calculation_method      fee_calculation_method NOT NULL,
                              amount                  NUMERIC(19,4) NOT NULL,
                              days_after_due          INTEGER NOT NULL DEFAULT 0,
                              description             VARCHAR(255),
                              active                  BOOLEAN NOT NULL DEFAULT TRUE,
                              created_by           VARCHAR(100),
                              updated_by           VARCHAR(100),
                              created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                              updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =========================================================
-- LOANS
-- =========================================================
CREATE TABLE loans (
                       id                      BIGSERIAL PRIMARY KEY,
                       loan_reference          VARCHAR(50) NOT NULL UNIQUE,
                       customer_id             BIGINT NOT NULL REFERENCES customers(id),
                       product_id              BIGINT NOT NULL REFERENCES loan_products(id),
                       principal_amount        NUMERIC(19,4) NOT NULL,
                       outstanding_balance     NUMERIC(19,4) NOT NULL,
                       loan_type               loan_type NOT NULL,
                       status                   loan_status NOT NULL DEFAULT 'OPEN',
                       billing_cycle_type      billing_cycle_type NOT NULL DEFAULT 'INDIVIDUAL',
                       consolidated_due_date   DATE,
                       disbursement_date       DATE NOT NULL,
                       due_date                DATE NOT NULL,
                       closed_date             DATE,
                       written_off_date        DATE,
                       notes                   TEXT,
                       created_by           VARCHAR(100),
                       updated_by           VARCHAR(100),
                       created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =========================================================
-- INSTALLMENTS (for installment-type loans)
-- =========================================================
CREATE TABLE loan_installments (
                                   id                      BIGSERIAL PRIMARY KEY,
                                   loan_id                 BIGINT NOT NULL REFERENCES loans(id),
                                   installment_number      INTEGER NOT NULL,
                                   principal_amount        NUMERIC(19,4) NOT NULL,
                                   outstanding_amount      NUMERIC(19,4) NOT NULL,
                                   due_date                DATE NOT NULL,
                                   paid_date               DATE,
                                   status                   loan_status NOT NULL DEFAULT 'OPEN',
                                   created_by           VARCHAR(100),
                                   updated_by           VARCHAR(100),
                                   created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                                   updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                                   UNIQUE (loan_id, installment_number)
);

-- =========================================================
-- LOAN FEES (fees applied on a loan instance)
-- =========================================================
CREATE TABLE loan_fees (
                           id                      BIGSERIAL PRIMARY KEY,
                           loan_id                 BIGINT NOT NULL REFERENCES loans(id),
                           product_fee_id          BIGINT REFERENCES product_fees(id),
                           fee_type                fee_type NOT NULL,
                           amount                  NUMERIC(19,4) NOT NULL,
                           applied_date            DATE NOT NULL,
                           paid                    BOOLEAN NOT NULL DEFAULT FALSE,
                           paid_date               DATE,
                           description             VARCHAR(255),
                           created_by           VARCHAR(100),
                           updated_by           VARCHAR(100),
                           created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                           updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =========================================================
-- REPAYMENTS
-- =========================================================
CREATE TABLE repayments (
                            id                      BIGSERIAL PRIMARY KEY,
                            repayment_reference     VARCHAR(50) NOT NULL UNIQUE,
                            loan_id                 BIGINT NOT NULL REFERENCES loans(id),
                            installment_id          BIGINT REFERENCES loan_installments(id),
                            amount                  NUMERIC(19,4) NOT NULL,
                            principal_paid          NUMERIC(19,4) NOT NULL DEFAULT 0,
                            fees_paid               NUMERIC(19,4) NOT NULL DEFAULT 0,
                            payment_date            DATE NOT NULL,
                            notes                   TEXT,
                            created_by           VARCHAR(100),
                            updated_by           VARCHAR(100),
                            created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                            updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =========================================================
-- NOTIFICATION TEMPLATES
-- =========================================================
CREATE TABLE notification_templates (
                                        id                      BIGSERIAL PRIMARY KEY,
                                        event                   notification_event_type NOT NULL,
                                        channel                 notification_channel NOT NULL,
                                        subject                 VARCHAR(255),
                                        body                    TEXT NOT NULL,
                                        active                  BOOLEAN NOT NULL DEFAULT TRUE,
                                        created_by           VARCHAR(100),
                                        updated_by           VARCHAR(100),
                                        created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                                        updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                                        UNIQUE (event, channel)
);

-- =========================================================
-- NOTIFICATION LOG
-- =========================================================
CREATE TABLE notifications (
                                   id                      BIGSERIAL PRIMARY KEY,
                                   customer_id             BIGINT NOT NULL REFERENCES customers(id),
                                   loan_id                 BIGINT REFERENCES loans(id),
                                   event                   notification_event_type NOT NULL,
                                   channel                 notification_channel NOT NULL,
                                   recipient               VARCHAR(255) NOT NULL,
                                   subject                 VARCHAR(255),
                                   message                 TEXT NOT NULL,
                                   status                  notification_status NOT NULL DEFAULT 'PENDING',
                                   error_message           TEXT,
                                   sent_at                 TIMESTAMP,
                                   created_by           VARCHAR(100),
                                   updated_by           VARCHAR(100),
                                   created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                                   updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =========================================================
-- INDEXES
-- =========================================================
CREATE INDEX idx_loans_customer_id     ON loans(customer_id);
CREATE INDEX idx_loans_status           ON loans(status);
CREATE INDEX idx_loans_due_date        ON loans(due_date);
CREATE INDEX idx_installments_loan_id  ON loan_installments(loan_id);
CREATE INDEX idx_installments_due_date ON loan_installments(due_date);
CREATE INDEX idx_repayments_loan_id    ON repayments(loan_id);
CREATE INDEX idx_notifs_customer   ON notifications(customer_id);
CREATE INDEX idx_notifs_loan       ON notifications(loan_id);