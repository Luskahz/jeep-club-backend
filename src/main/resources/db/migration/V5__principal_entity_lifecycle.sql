-- BACK-512: six bounded-context availability lifecycles; process enums and V1-V4 remain unchanged.
-- Requires a coordinated stop of application writes. Back up/restore DB before rollout.
-- Own history is independent of operational rows. No financial facts or media are deleted.
-- DDL is non-transactional in MySQL. On failure reconcile/restore; never conceal it with repair/clean.

DROP TEMPORARY TABLE IF EXISTS back512_preflight;
CREATE TEMPORARY TABLE back512_preflight (check_name VARCHAR(64) PRIMARY KEY, invalid_count BIGINT NOT NULL, CONSTRAINT ck_back512_preflight CHECK (invalid_count = 0));
INSERT INTO back512_preflight
SELECT 'ROOT_OR_CONTRADICTORY_ROLE', COUNT(*) FROM authorization_roles
WHERE (kind = 'ROOT' AND status <> 'ACTIVE')
   OR (status <> 'DELETED' AND deleted_at IS NOT NULL);
INSERT INTO back512_preflight
SELECT 'USER_AVAILABILITY_TIMESTAMPS', COUNT(*) FROM identity_users
WHERE (status IN ('DISABLED','INACTIVE','ARCHIVED') AND disabled_at IS NULL)
   OR (status = 'ACTIVE' AND disabled_at IS NOT NULL);

SET @back512_ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE identity_users ADD COLUMN archived_at DATETIME(6) NULL', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'identity_users' AND column_name = 'archived_at');
PREPARE back512_statement FROM @back512_ddl;
EXECUTE back512_statement;
DEALLOCATE PREPARE back512_statement;
SET @back512_ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE authorization_roles ADD COLUMN archived_at DATETIME(6) NULL', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'authorization_roles' AND column_name = 'archived_at');
PREPARE back512_statement FROM @back512_ddl;
EXECUTE back512_statement;
DEALLOCATE PREPARE back512_statement;
SET @back512_ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE dependents_dependent ADD COLUMN archived_at DATETIME(6) NULL', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dependents_dependent' AND column_name = 'archived_at');
PREPARE back512_statement FROM @back512_ddl;
EXECUTE back512_statement;
DEALLOCATE PREPARE back512_statement;
SET @back512_ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE dependents_dependent_history ADD COLUMN archived_at DATETIME(6) NULL', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dependents_dependent_history' AND column_name = 'archived_at');
PREPARE back512_statement FROM @back512_ddl;
EXECUTE back512_statement;
DEALLOCATE PREPARE back512_statement;
SET @back512_ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE tools ADD COLUMN archived_at DATETIME(6) NULL', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tools' AND column_name = 'archived_at');
PREPARE back512_statement FROM @back512_ddl;
EXECUTE back512_statement;
DEALLOCATE PREPARE back512_statement;
SET @back512_ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE tools_tool_history ADD COLUMN archived_at DATETIME(6) NULL', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tools_tool_history' AND column_name = 'archived_at');
PREPARE back512_statement FROM @back512_ddl;
EXECUTE back512_statement;
DEALLOCATE PREPARE back512_statement;
SET @back512_ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE vehicles_vehicle ADD COLUMN archived_at DATETIME(6) NULL', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'vehicles_vehicle' AND column_name = 'archived_at');
PREPARE back512_statement FROM @back512_ddl;
EXECUTE back512_statement;
DEALLOCATE PREPARE back512_statement;
SET @back512_ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE vehicles_vehicle_history ADD COLUMN archived_at DATETIME(6) NULL', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'vehicles_vehicle_history' AND column_name = 'archived_at');
PREPARE back512_statement FROM @back512_ddl;
EXECUTE back512_statement;
DEALLOCATE PREPARE back512_statement;
SET @back512_ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE vehicles_vehicle ADD COLUMN legacy_deleted BIT NOT NULL DEFAULT 0', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'vehicles_vehicle' AND column_name = 'legacy_deleted');
PREPARE back512_statement FROM @back512_ddl;
EXECUTE back512_statement;
DEALLOCATE PREPARE back512_statement;
SET @back512_ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE vehicles_vehicle_history ADD COLUMN legacy_deleted BIT NOT NULL DEFAULT 0', 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'vehicles_vehicle_history' AND column_name = 'legacy_deleted');
PREPARE back512_statement FROM @back512_ddl;
EXECUTE back512_statement;
DEALLOCATE PREPARE back512_statement;

CREATE TABLE IF NOT EXISTS identity_user_deletion_history (
    original_id BIGINT NOT NULL PRIMARY KEY,
    previous_status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    disabled_at DATETIME(6) NULL,
    archived_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    deleted_by_user_id BIGINT NOT NULL,
    deleted_at DATETIME(6) NOT NULL,
    reason VARCHAR(40) NOT NULL,
    schema_version INT NOT NULL,
    INDEX ix_user_deletion_history_time (deleted_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS authorization_user_role_removal_history (
    original_id BIGINT NOT NULL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    deleted_by_user_id BIGINT NOT NULL,
    deleted_at DATETIME(6) NOT NULL,
    reason VARCHAR(40) NOT NULL,
    schema_version INT NOT NULL,
    INDEX ix_user_role_removal_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS authorization_role_deletion_history (
    original_id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    kind VARCHAR(30) NOT NULL,
    previous_status VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    archived_at DATETIME(6) NULL,
    deleted_by_user_id BIGINT NULL,
    deleted_at DATETIME(6) NOT NULL,
    reason VARCHAR(40) NOT NULL,
    inferred_time BIT NOT NULL,
    schema_version INT NOT NULL,
    INDEX ix_role_deletion_history_time (deleted_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS authorization_role_permission_deletion_history (
    original_id BIGINT NOT NULL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    permission_code VARCHAR(120) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    INDEX ix_role_permission_deletion_role (role_id),
    CONSTRAINT fk_role_permission_deletion_history FOREIGN KEY (role_id) REFERENCES authorization_role_deletion_history(original_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS authorization_user_role_deletion_history (
    original_id BIGINT NOT NULL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    INDEX ix_user_role_deletion_role (role_id),
    CONSTRAINT fk_user_role_deletion_history FOREIGN KEY (role_id) REFERENCES authorization_role_deletion_history(original_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS billing_charge_definition_deletion_history (
    original_id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    default_amount DECIMAL(15,2) NOT NULL,
    recurrence_type VARCHAR(30) NOT NULL,
    required BIT NOT NULL,
    payment_acceptance_policy VARCHAR(40) NOT NULL,
    late_payment_grace_days INT NULL,
    previous_status VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    archived_at DATETIME(6) NULL,
    deleted_by_user_id BIGINT NOT NULL,
    deleted_at DATETIME(6) NOT NULL,
    reason VARCHAR(40) NOT NULL,
    schema_version INT NOT NULL,
    INDEX ix_definition_deletion_history_time (deleted_at)
) ENGINE=InnoDB;

ALTER TABLE identity_users MODIFY COLUMN status ENUM ('ACTIVE','DISABLED','INACTIVE','ARCHIVED') NOT NULL;
ALTER TABLE dependents_dependent MODIFY COLUMN status ENUM ('ACTIVE','DISABLED','INACTIVE','ARCHIVED') NOT NULL;
ALTER TABLE dependents_dependent_history MODIFY COLUMN status ENUM ('ACTIVE','DISABLED','INACTIVE','ARCHIVED') NOT NULL;
ALTER TABLE authorization_roles MODIFY COLUMN status ENUM ('ACTIVE','DELETED','INACTIVE','ARCHIVED') NOT NULL;
ALTER TABLE tools MODIFY COLUMN status ENUM ('ACTIVE','INACTIVE','ARCHIVED') NOT NULL;
ALTER TABLE tools_tool_history MODIFY COLUMN status ENUM ('ACTIVE','INACTIVE','ARCHIVED') NOT NULL;
ALTER TABLE vehicles_vehicle MODIFY COLUMN status ENUM ('ACTIVE','SOFT_DELETED','INACTIVE','ARCHIVED') NOT NULL;
ALTER TABLE vehicles_vehicle_history MODIFY COLUMN status ENUM ('ACTIVE','SOFT_DELETED','INACTIVE','ARCHIVED') NOT NULL;

-- Fail on conflicting pre-existing snapshots; controlled replay may preserve identical facts only.
INSERT INTO back512_preflight
SELECT 'ROLE_HISTORY_COLLISION', COUNT(*)
FROM authorization_roles r JOIN authorization_role_deletion_history h ON h.original_id = r.id
WHERE r.status = 'DELETED'
  AND (NOT (h.name <=> r.name) OR NOT (h.description <=> r.description)
       OR h.kind <> CAST(r.kind AS CHAR) OR h.previous_status <> 'DELETED'
       OR NOT (h.created_at <=> r.created_at) OR NOT (h.updated_at <=> r.updated_at)
       OR NOT (h.deleted_at <=> COALESCE(r.deleted_at,r.updated_at,r.created_at)));
INSERT INTO back512_preflight
SELECT 'ROLE_PERMISSION_HISTORY_COLLISION', COUNT(*)
FROM authorization_role_permissions rp
JOIN authorization_roles r ON r.id = rp.role_id
JOIN authorization_role_permission_deletion_history h ON h.original_id = rp.id
WHERE r.status = 'DELETED'
  AND (h.role_id <> rp.role_id OR h.permission_id <> rp.permission_id OR NOT (h.created_at <=> rp.created_at));
INSERT INTO back512_preflight
SELECT 'USER_ROLE_HISTORY_COLLISION', COUNT(*)
FROM authorization_user_roles ur
JOIN authorization_roles r ON r.id = ur.role_id
JOIN authorization_user_role_deletion_history h ON h.original_id = ur.id
WHERE r.status = 'DELETED'
  AND (h.role_id <> ur.role_id OR h.user_id <> ur.user_id OR NOT (h.created_at <=> ur.created_at));

INSERT INTO authorization_role_deletion_history
(original_id,name,description,kind,previous_status,created_at,updated_at,archived_at,deleted_by_user_id,deleted_at,reason,inferred_time,schema_version)
SELECT r.id,r.name,r.description,CAST(r.kind AS CHAR),'DELETED',r.created_at,r.updated_at,NULL,NULL,
       COALESCE(r.deleted_at,r.updated_at,r.created_at),'LEGACY_ROLE_DELETION',r.deleted_at IS NULL,1
FROM authorization_roles r
WHERE r.status = 'DELETED'
  AND NOT EXISTS (SELECT 1 FROM authorization_role_deletion_history h WHERE h.original_id = r.id);
INSERT INTO authorization_role_permission_deletion_history
(original_id,role_id,permission_id,permission_code,created_at)
SELECT rp.id,rp.role_id,rp.permission_id,CAST(p.code AS CHAR),rp.created_at
FROM authorization_role_permissions rp
JOIN authorization_roles r ON r.id = rp.role_id
JOIN authorization_permissions p ON p.id = rp.permission_id
WHERE r.status = 'DELETED'
  AND NOT EXISTS (SELECT 1 FROM authorization_role_permission_deletion_history h WHERE h.original_id = rp.id);
INSERT INTO authorization_user_role_deletion_history
(original_id,role_id,user_id,created_at)
SELECT ur.id,ur.role_id,ur.user_id,ur.created_at
FROM authorization_user_roles ur JOIN authorization_roles r ON r.id = ur.role_id
WHERE r.status = 'DELETED'
  AND NOT EXISTS (SELECT 1 FROM authorization_user_role_deletion_history h WHERE h.original_id = ur.id);
-- Remove operational associations only after every source fact has its own snapshot.
DELETE rp FROM authorization_role_permissions rp
JOIN authorization_roles r ON r.id = rp.role_id
JOIN authorization_role_permission_deletion_history h ON h.original_id = rp.id
WHERE r.status = 'DELETED';
DELETE ur FROM authorization_user_roles ur
JOIN authorization_roles r ON r.id = ur.role_id
JOIN authorization_user_role_deletion_history h ON h.original_id = ur.id
WHERE r.status = 'DELETED';
DELETE r FROM authorization_roles r
JOIN authorization_role_deletion_history h ON h.original_id = r.id
WHERE r.status = 'DELETED';

UPDATE identity_users SET status = 'INACTIVE' WHERE status = 'DISABLED';
UPDATE dependents_dependent SET status = 'INACTIVE' WHERE status = 'DISABLED';
UPDATE dependents_dependent_history SET status = 'INACTIVE' WHERE status = 'DISABLED';
-- An inferred archival time is compatibility metadata, not a claim about the historical deletion instant.
UPDATE vehicles_vehicle SET legacy_deleted = 1, archived_at = COALESCE(archived_at,updated_at,created_at), status = 'ARCHIVED' WHERE status = 'SOFT_DELETED';
UPDATE vehicles_vehicle_history SET legacy_deleted = 1, archived_at = COALESCE(archived_at,updated_at,created_at), status = 'ARCHIVED' WHERE status = 'SOFT_DELETED';

ALTER TABLE identity_users MODIFY COLUMN status ENUM ('ACTIVE','ARCHIVED','INACTIVE') NOT NULL;
ALTER TABLE dependents_dependent MODIFY COLUMN status ENUM ('ACTIVE','ARCHIVED','INACTIVE') NOT NULL;
ALTER TABLE dependents_dependent_history MODIFY COLUMN status ENUM ('ACTIVE','ARCHIVED','INACTIVE') NOT NULL;
ALTER TABLE authorization_roles MODIFY COLUMN status ENUM ('ACTIVE','ARCHIVED','INACTIVE') NOT NULL;
ALTER TABLE tools MODIFY COLUMN status ENUM ('ACTIVE','ARCHIVED','INACTIVE') NOT NULL;
ALTER TABLE tools_tool_history MODIFY COLUMN status ENUM ('ACTIVE','ARCHIVED','INACTIVE') NOT NULL;
ALTER TABLE vehicles_vehicle MODIFY COLUMN status ENUM ('ACTIVE','ARCHIVED','INACTIVE') NOT NULL;
ALTER TABLE vehicles_vehicle_history MODIFY COLUMN status ENUM ('ACTIVE','ARCHIVED','INACTIVE') NOT NULL;

-- Append only: preserve every V4 permission ENUM position and grant CUSTOM roles nothing implicitly.
ALTER TABLE authorization_permissions MODIFY COLUMN code ENUM ('AUTHENTICATION_EXPORT','AUTHENTICATION_PASSWORD_RECOVERY_CANCEL','AUTHENTICATION_PASSWORD_RECOVERY_READ','AUTHENTICATION_REFRESH_TOKEN_READ','AUTHENTICATION_REFRESH_TOKEN_REVOKE','AUTHENTICATION_SESSION_LOGOUT','AUTHENTICATION_SESSION_READ','AUTHENTICATION_USER_PASSWORD_RESET_LINK_GENERATE','AUTHENTICATION_USER_TEMPORARY_PASSWORD_GENERATE','AUTHORIZATION_EXPORT','AUTHORIZATION_PERMISSION_ASSIGN','AUTHORIZATION_PERMISSION_READ','AUTHORIZATION_PERMISSION_REVOKE','AUTHORIZATION_ROLE_CREATE','AUTHORIZATION_ROLE_DELETE','AUTHORIZATION_ROLE_DISABLE','AUTHORIZATION_ROLE_ENABLE','AUTHORIZATION_ROLE_READ','AUTHORIZATION_ROLE_UPDATE','AUTHORIZATION_USER_ROLE_ASSIGN','AUTHORIZATION_USER_ROLE_READ','AUTHORIZATION_USER_ROLE_REVOKE','BILLING_CHARGE_ASSIGNMENT_CREATE','BILLING_CHARGE_ASSIGNMENT_READ','BILLING_CHARGE_ASSIGNMENT_UPDATE','BILLING_CHARGE_CYCLE_ARCHIVE','BILLING_CHARGE_CYCLE_CANCEL','BILLING_CHARGE_CYCLE_FINISH','BILLING_CHARGE_CYCLE_GENERATE','BILLING_CHARGE_CYCLE_READ','BILLING_CHARGE_DEFINITION_CREATE','BILLING_CHARGE_DEFINITION_READ','BILLING_CHARGE_DEFINITION_UPDATE','BILLING_EXPORT','BILLING_MEMBER_CHARGE_CANCEL','BILLING_MEMBER_CHARGE_READ','BILLING_MEMBER_CHARGE_UPDATE','BILLING_PAYMENT_CONFIRM','BILLING_PAYMENT_READ','BILLING_PAYMENT_REJECT','BILLING_REFUND_APPROVE','BILLING_REFUND_CANCEL','BILLING_REFUND_EXPIRE','BILLING_REFUND_MARK_AS_REFUNDED','BILLING_REFUND_READ','BILLING_REFUND_REJECT','DEPENDENTS_DEPENDENT_EXPORT','DEPENDENTS_DEPENDENT_READ','HEALTH_MEDICAL_PROFILE_DELETE','HEALTH_MEDICAL_PROFILE_EXPORT','HEALTH_MEDICAL_PROFILE_READ','HEALTH_MEDICAL_PROFILE_UPDATE','IDENTITY_USER_CREATE','IDENTITY_USER_DISABLE','IDENTITY_USER_ENABLE','IDENTITY_USER_EXPORT','IDENTITY_USER_READ','IDENTITY_USER_UPDATE','MEMBERSHIP_BILLING_CONFIGURATION_READ','MEMBERSHIP_BILLING_CONFIGURATION_UPDATE','MEMBERSHIP_EXPORT','MEMBERSHIP_MEMBERSHIP_APPLICANT_BLOCK','MEMBERSHIP_MEMBERSHIP_APPLICANT_UNBLOCK','MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE','MEMBERSHIP_MEMBERSHIP_REQUEST_INVITE_RESEND','MEMBERSHIP_MEMBERSHIP_REQUEST_READ','MEMBERSHIP_MEMBERSHIP_REQUEST_REJECT','PUBLICATIONS_COMMENT_CREATE','PUBLICATIONS_COMMENT_READ','PUBLICATIONS_EVENT_ADMIN_DASHBOARD_READ','PUBLICATIONS_EVENT_CANCEL','PUBLICATIONS_EVENT_CREATE','PUBLICATIONS_EVENT_DELETE','PUBLICATIONS_EVENT_FINISH','PUBLICATIONS_EVENT_GUEST_ADMIN_CREATE','PUBLICATIONS_EVENT_GUEST_ADMIN_READ','PUBLICATIONS_EVENT_GUEST_APPROVE','PUBLICATIONS_EVENT_GUEST_REJECT','PUBLICATIONS_EVENT_GUEST_REQUEST_CREATE','PUBLICATIONS_EVENT_GUEST_REQUEST_READ','PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ','PUBLICATIONS_EVENT_PUBLISH','PUBLICATIONS_EVENT_READ','PUBLICATIONS_EVENT_READ_ADMIN','PUBLICATIONS_EVENT_REGISTER','PUBLICATIONS_EVENT_REGISTRATION_CANCEL','PUBLICATIONS_EVENT_REGISTRATION_READ','PUBLICATIONS_EVENT_RIDE_OFFER_READ','PUBLICATIONS_EVENT_RIDE_OFFER_RESPOND','PUBLICATIONS_EVENT_RIDE_OFFER_SELECT','PUBLICATIONS_EVENT_UPDATE','PUBLICATIONS_EXPORT','PUBLICATIONS_FEED_READ','PUBLICATIONS_INTERACTION_LIKE','PUBLICATIONS_NOTICE_ARCHIVE','PUBLICATIONS_NOTICE_CREATE','PUBLICATIONS_NOTICE_DELETE','PUBLICATIONS_NOTICE_PUBLISH','PUBLICATIONS_NOTICE_READ','PUBLICATIONS_NOTICE_UPDATE','PUBLICATIONS_PUBLICATION_READ','PUBLICATIONS_SERVICE_ADMIN_DELETE','PUBLICATIONS_SERVICE_ADMIN_READ','PUBLICATIONS_SERVICE_CHANGE_REQUEST_ADMIN_READ','PUBLICATIONS_SERVICE_CHANGE_REQUEST_APPROVE','PUBLICATIONS_SERVICE_CHANGE_REQUEST_CREATE','PUBLICATIONS_SERVICE_CHANGE_REQUEST_READ','PUBLICATIONS_SERVICE_CHANGE_REQUEST_REJECT','PUBLICATIONS_SERVICE_DELETE','PUBLICATIONS_SERVICE_READ','PUBLICATIONS_SERVICE_REQUEST_ADMIN_READ','PUBLICATIONS_SERVICE_REQUEST_APPROVE','PUBLICATIONS_SERVICE_REQUEST_CREATE','PUBLICATIONS_SERVICE_REQUEST_READ','PUBLICATIONS_SERVICE_REQUEST_REJECT','TOOLS_TOOL_ACTIVATE','TOOLS_TOOL_CREATE','TOOLS_TOOL_DEACTIVATE','TOOLS_TOOL_DELETE','TOOLS_TOOL_EXPORT','TOOLS_TOOL_READ','TOOLS_TOOL_UPDATE','VEHICLES_VEHICLE_CREATE','VEHICLES_VEHICLE_DELETE','VEHICLES_VEHICLE_EXPORT','VEHICLES_VEHICLE_READ','VEHICLES_VEHICLE_UPDATE','MEMBERSHIP_MEMBER_REGISTRATION_MANAGE','MEMBERSHIP_MEMBER_READ','MEMBERSHIP_MEMBER_AUDIT_READ','MEMBERSHIP_MEMBER_NUMBER_RESERVE','MEMBERSHIP_MEMBER_NUMBER_ISSUE','MEMBERSHIP_MEMBER_IMPORT','IDENTITY_USER_ARCHIVE','IDENTITY_USER_UNARCHIVE','IDENTITY_USER_DELETE','IDENTITY_USER_HISTORY_READ','AUTHORIZATION_ROLE_ARCHIVE','AUTHORIZATION_ROLE_UNARCHIVE','AUTHORIZATION_ROLE_HISTORY_READ','DEPENDENTS_DEPENDENT_ACTIVATE','DEPENDENTS_DEPENDENT_DEACTIVATE','DEPENDENTS_DEPENDENT_ARCHIVE','DEPENDENTS_DEPENDENT_UNARCHIVE','DEPENDENTS_DEPENDENT_DELETE','TOOLS_TOOL_ARCHIVE','TOOLS_TOOL_UNARCHIVE','VEHICLES_VEHICLE_ACTIVATE','VEHICLES_VEHICLE_DEACTIVATE','VEHICLES_VEHICLE_ARCHIVE','VEHICLES_VEHICLE_UNARCHIVE','BILLING_CHARGE_DEFINITION_DELETE','BILLING_CHARGE_DEFINITION_HISTORY_READ') NOT NULL;
DROP TEMPORARY TABLE back512_preflight;
