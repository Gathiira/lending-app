-- =========================================================
-- V5__credit_limit_adjustment.sql
-- credit limit adjustments
-- =========================================================

CREATE TABLE credit_limit_adjustment (
                                         id                BIGSERIAL       PRIMARY KEY,
                                         credit_limit_id   BIGINT          NOT NULL,
                                         amount            NUMERIC(19, 2)  NOT NULL,
                                         type              VARCHAR(20)     NOT NULL,
                                         reason            VARCHAR(500),
                                         created_at        TIMESTAMP,
                                         updated_at        TIMESTAMP,
                                         created_by        VARCHAR(255),
                                         updated_by        VARCHAR(255),
                                         CONSTRAINT fk_adjustment_credit_limit FOREIGN KEY (credit_limit_id) REFERENCES credit_limit (id)
);

CREATE INDEX idx_adjustment_credit_limit_id ON credit_limit_adjustment (credit_limit_id);