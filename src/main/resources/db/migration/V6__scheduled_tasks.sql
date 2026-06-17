CREATE TABLE scheduled_tasks (
    id              BIGSERIAL    PRIMARY KEY,
    task_name       VARCHAR(255) NOT NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    error_message   TEXT,
    records_processed INTEGER
);
