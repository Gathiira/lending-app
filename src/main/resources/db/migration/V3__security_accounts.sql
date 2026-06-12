-- =========================================================
-- V3__security_and_audit.sql
-- User accounts, roles, and audit columns
-- =========================================================
-- =========================================================
-- USER ACCOUNTS
-- Unified auth table; staff rows have no customer_id,
-- customer rows link back to the customers table.
-- =========================================================
CREATE TABLE user_accounts (
                               id                  BIGSERIAL PRIMARY KEY,
                               username            VARCHAR(100) NOT NULL UNIQUE,
                               password       VARCHAR(255) NOT NULL,
                               role                VARCHAR(100) NOT NULL DEFAULT 'CUSTOMER',
                               status              VARCHAR(100) NOT NULL DEFAULT 'ACTIVE',
                               customer_id         BIGINT REFERENCES customers(id),   -- NULL for STAFF / ADMIN
                               last_login_at       TIMESTAMP,
                               password_reset_token VARCHAR(255),
                               token_expiry        TIMESTAMP,
                               created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                               updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                               created_by          VARCHAR(100),
                               updated_by          VARCHAR(100)
);

CREATE INDEX idx_user_accounts_username    ON user_accounts(username);
CREATE INDEX idx_user_accounts_customer_id ON user_accounts(customer_id);

-- =========================================================
-- LOAN APPROVAL WORKFLOW
-- Staff must approve customer loan applications
-- before disbursement.
-- =========================================================

CREATE TABLE loan_approvals (
                                id              BIGSERIAL PRIMARY KEY,
                                loan_id         BIGINT NOT NULL REFERENCES loans(id),
                                reviewed_by     BIGINT REFERENCES user_accounts(id),
                                status          VARCHAR(100) NOT NULL DEFAULT 'PENDING',
                                notes           TEXT,
                                reviewed_at     TIMESTAMP,
                                created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                                updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                                created_by      VARCHAR(100),
                                updated_by      VARCHAR(100)
);

-- =========================================================
-- CREDIT LIMIT APPROVAL WORKFLOW
-- =========================================================
CREATE TABLE credit_limit_requests (
                                       id                  BIGSERIAL PRIMARY KEY,
                                       customer_id         BIGINT NOT NULL REFERENCES customers(id),
                                       approved_limit       NUMERIC(19,4) NOT NULL,
                                       reason              TEXT,
                                       file_url              TEXT,
                                       status              VARCHAR(100) NOT NULL DEFAULT 'PENDING',
                                       reviewed_by         BIGINT REFERENCES user_accounts(id),
                                       review_notes        TEXT,
                                       reviewed_at         TIMESTAMP,
                                       created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                                       updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                                       created_by          VARCHAR(100),
                                       updated_by          VARCHAR(100)
);

-- =========================================================
-- CREDIT LIMIT WORKFLOW
-- =========================================================

CREATE TABLE credit_limit(
                                       id                  BIGSERIAL PRIMARY KEY,
                                       customer_id         BIGINT NOT NULL REFERENCES customers(id),
                                       request_id         BIGINT NOT NULL REFERENCES credit_limit_requests(id),
                                       product_id         BIGINT NOT NULL REFERENCES loan_products(id),
                                       credit_limit       NUMERIC(19,4) NOT NULL,
                                       frozen_limit       NUMERIC(19,4) NOT NULL,
                                       available_limit       NUMERIC(19,4) NOT NULL,
                                       status              VARCHAR(100) NOT NULL DEFAULT 'ACTIVE',
                                       created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                                       updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                                       created_by          VARCHAR(100),
                                       updated_by          VARCHAR(100)
);