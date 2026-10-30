CREATE TABLE membership_rejection_notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    decision_version BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    state VARCHAR(16) NOT NULL,
    automatic_attempts INT NOT NULL,
    next_attempt_at DATETIME(6) NULL,
    lease_until DATETIME(6) NULL,
    current_attempt_id BIGINT NULL,
    last_origin VARCHAR(16) NULL,
    last_result VARCHAR(32) NULL,
    last_attempt_at DATETIME(6) NULL,
    smtp_accepted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_membership_rejection_decision UNIQUE (application_id, type),
    CONSTRAINT fk_membership_rejection_application FOREIGN KEY (application_id) REFERENCES membership_applications(id),
    CONSTRAINT ck_membership_rejection_type CHECK (type = 'MEMBERSHIP_REJECTED'),
    CONSTRAINT ck_membership_rejection_state CHECK (state IN ('PENDING','PROCESSING','SENT','FAILED')),
    CONSTRAINT ck_membership_rejection_count CHECK (automatic_attempts BETWEEN 0 AND 3),
    CONSTRAINT ck_membership_rejection_lease CHECK (
        (state = 'PROCESSING' AND lease_until IS NOT NULL AND current_attempt_id IS NOT NULL) OR
        (state <> 'PROCESSING' AND lease_until IS NULL AND current_attempt_id IS NULL)),
    CONSTRAINT ck_membership_rejection_pending CHECK (state <> 'PENDING' OR next_attempt_at IS NOT NULL),
    INDEX ix_membership_rejection_due (state, next_attempt_at, id),
    INDEX ix_membership_rejection_lease (state, lease_until, id)
);

CREATE TABLE membership_rejection_notification_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    notification_id BIGINT NOT NULL,
    origin VARCHAR(16) NOT NULL,
    actor_id BIGINT NULL,
    automatic_number INT NULL,
    result VARCHAR(32) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_membership_rejection_attempt FOREIGN KEY (notification_id) REFERENCES membership_rejection_notifications(id),
    CONSTRAINT uk_membership_rejection_automatic UNIQUE (notification_id, automatic_number),
    CONSTRAINT ck_membership_rejection_origin CHECK (
        (origin = 'AUTOMATIC' AND automatic_number IS NOT NULL AND automatic_number BETWEEN 1 AND 3 AND actor_id IS NULL) OR
        (origin = 'MANUAL' AND automatic_number IS NULL AND actor_id IS NOT NULL)),
    CONSTRAINT ck_membership_rejection_result CHECK (result IN ('PROCESSING','SMTP_ACCEPTED','SIMULATED','SMTP_FAILED','UNKNOWN','INELIGIBLE')),
    INDEX ix_membership_rejection_history (notification_id, id)
);
