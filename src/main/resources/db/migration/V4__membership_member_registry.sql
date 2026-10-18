-- Additive only. No existing identity, application, credential or financial row is changed.
CREATE TABLE membership_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    identity_user_id BIGINT NOT NULL,
    membership_number VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_membership_member_identity UNIQUE (identity_user_id),
    CONSTRAINT uk_membership_member_number UNIQUE (membership_number),
    CONSTRAINT fk_membership_member_identity FOREIGN KEY (identity_user_id) REFERENCES identity_users(id)
) ENGINE=InnoDB;

CREATE TABLE membership_number_claims (
    claim_key VARCHAR(64) NOT NULL,
    membership_number VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    member_id BIGINT NULL,
    actor_user_id BIGINT NOT NULL,
    claimed_at DATETIME(6) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    PRIMARY KEY (claim_key),
    CONSTRAINT uk_membership_claim_number UNIQUE (membership_number),
    CONSTRAINT fk_membership_claim_member FOREIGN KEY (member_id) REFERENCES membership_members(id)
) ENGINE=InnoDB;

CREATE TABLE membership_member_audits (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NULL,
    previous_number VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
    new_number VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    actor_user_id BIGINT NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    operation VARCHAR(32) NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    PRIMARY KEY (id),
    INDEX ix_membership_audit_member (member_id, id),
    CONSTRAINT fk_membership_audit_member FOREIGN KEY (member_id) REFERENCES membership_members(id)
) ENGINE=InnoDB;

CREATE TABLE membership_registry_sequence (
    id BIGINT NOT NULL,
    next_value BIGINT NULL,
    format_signature VARCHAR(200) NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;
-- Coordination state only; no initial membership number, no Member and no backfill.
INSERT INTO membership_registry_sequence (id, next_value, format_signature, version) VALUES (1, NULL, NULL, 0);
