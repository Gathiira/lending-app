-- =========================================================
-- V3__security_and_audit.sql
-- User accounts, roles, and audit columns
-- =========================================================

-- ENUMS
CREATE TYPE user_role AS ENUM ('CUSTOMER', 'STAFF', 'ADMIN');
CREATE TYPE account_status AS ENUM ('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING_VERIFICATION');

-- =========================================================
-- USER ACCOUNTS
-- Unified auth table; staff rows have no customer_id,
-- customer rows link back to the customers table.
-- =========================================================
CREATE TABLE user_accounts (
                               id                  BIGSERIAL PRIMARY KEY,
                               username            VARCHAR(100) NOT NULL UNIQUE,
                               password       VARCHAR(255) NOT NULL,
                               role                user_role NOT NULL DEFAULT 'CUSTOMER',
                               status              account_status NOT NULL DEFAULT 'ACTIVE',
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
CREATE TYPE approval_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

CREATE TABLE loan_approvals (
                                id              BIGSERIAL PRIMARY KEY,
                                loan_id         BIGINT NOT NULL REFERENCES loans(id),
                                reviewed_by     BIGINT REFERENCES user_accounts(id),
                                status          approval_status NOT NULL DEFAULT 'PENDING',
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
                                       requested_limit     NUMERIC(19,4) NOT NULL,
                                       current_limit       NUMERIC(19,4) NOT NULL,
                                       reason              TEXT,
                                       status              approval_status NOT NULL DEFAULT 'PENDING',
                                       reviewed_by         BIGINT REFERENCES user_accounts(id),
                                       review_notes        TEXT,
                                       reviewed_at         TIMESTAMP,
                                       created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                                       updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                                       created_by          VARCHAR(100),
                                       updated_by          VARCHAR(100)
);

-- =========================================================
-- LOAN STATUS: add PENDING_APPROVAL state
-- =========================================================
ALTER TYPE loan_status ADD VALUE IF NOT EXISTS 'PENDING_APPROVAL';