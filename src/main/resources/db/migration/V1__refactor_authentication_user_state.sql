-- Existing installations are baselined at version 0. This migration preserves
-- all authentication_users rows while correcting column names and separating
-- administrative, authentication and credential state.

SET @rename_birth_date = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'authentication_users'
       AND column_name = 'birth_data') > 0,
    'ALTER TABLE authentication_users RENAME COLUMN birth_data TO birth_date',
    'SELECT 1'
);
PREPARE rename_birth_date_statement FROM @rename_birth_date;
EXECUTE rename_birth_date_statement;
DEALLOCATE PREPARE rename_birth_date_statement;

SET @rename_password_changed_at = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'authentication_users'
       AND column_name = 'password_change_at') > 0,
    'ALTER TABLE authentication_users RENAME COLUMN password_change_at TO password_changed_at',
    'SELECT 1'
);
PREPARE rename_password_changed_at_statement FROM @rename_password_changed_at;
EXECUTE rename_password_changed_at_statement;
DEALLOCATE PREPARE rename_password_changed_at_statement;

ALTER TABLE authentication_users
    ADD COLUMN account_status VARCHAR(32) NULL,
    ADD COLUMN authentication_status VARCHAR(32) NULL,
    ADD COLUMN credential_status VARCHAR(32) NULL;

UPDATE authentication_users
SET account_status = CASE status
        WHEN 'DISABLED' THEN 'DISABLED'
        ELSE 'ACTIVE'
    END,
    authentication_status = CASE status
        WHEN 'LOCKED' THEN 'LOCKED'
        ELSE 'ENABLED'
    END,
    credential_status = CASE status
        WHEN 'PENDING_FIRST_ACCESS' THEN 'PENDING_FIRST_ACCESS'
        WHEN 'CHANGE_PASSWORD_REQUIRED' THEN 'CHANGE_REQUIRED'
        ELSE 'PERMANENT'
    END,
    failed_login_attempts = CASE
        WHEN status = 'LOCKED' THEN GREATEST(failed_login_attempts, 5)
        ELSE failed_login_attempts
    END,
    disabled_at = CASE
        WHEN status = 'DISABLED' THEN COALESCE(disabled_at, updated_at, created_at, CURRENT_TIMESTAMP)
        ELSE NULL
    END;

ALTER TABLE authentication_users
    MODIFY account_status VARCHAR(32) NOT NULL,
    MODIFY authentication_status VARCHAR(32) NOT NULL,
    MODIFY credential_status VARCHAR(32) NOT NULL;
