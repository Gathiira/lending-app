CREATE TABLE notification_tracker (
    id                BIGSERIAL       PRIMARY KEY,
    loan_id           BIGINT          NOT NULL REFERENCES loans(id),
    event             VARCHAR(50)     NOT NULL,
    notification_date DATE            NOT NULL,
    status            VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    sent_at           TIMESTAMP,
    error_message     TEXT,
    created_at        TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    UNIQUE (loan_id, event, notification_date)
);
