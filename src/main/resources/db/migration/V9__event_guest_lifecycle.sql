-- BACK-537. Stop mixed old/new writers. Preflight precedes irreversible MySQL DDL.
-- No deletion, CPF deduplication, fabricated review timestamp or CUSTOM grants.
DELIMITER $$
CREATE PROCEDURE back537_preflight()
BEGIN
    IF EXISTS (SELECT 1 FROM event_guest_requests g WHERE g.administrative=b'0'
        AND (g.vehicle_id IS NULL OR (SELECT COUNT(*) FROM event_registrations r
            WHERE r.event_id=g.event_id AND r.user_id=g.requester_user_id) <> 1)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='BACK-537: personal guest origin/vehicle cannot be reconciled; preserve rows and investigate';
    END IF;
    IF EXISTS (SELECT 1 FROM event_guest_requests g WHERE g.status='APPROVED'
        AND (g.vehicle_id IS NULL OR (SELECT COUNT(*) FROM event_registration_vehicles v
            JOIN event_registrations r ON r.id=v.registration_id
            WHERE r.event_id=g.event_id AND v.vehicle_id=g.vehicle_id AND r.status='CONFIRMED') <> 1)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='BACK-537: approved guest transport is missing/ambiguous; reconcile before migration';
    END IF;
    IF EXISTS (SELECT 1 FROM event_guest_requests WHERE administrative=b'1' AND status='PENDING' AND vehicle_id IS NOT NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='BACK-537: administrative pending guest has unexpected transport';
    END IF;
    IF EXISTS (SELECT 1 FROM event_ride_offers o LEFT JOIN event_guest_requests g ON g.id=o.guest_request_id
        LEFT JOIN event_registrations r ON r.id=o.registration_id
        WHERE o.status='SELECTED' AND (g.id IS NULL OR g.status<>'APPROVED' OR g.administrative=b'0'
            OR g.event_id<>o.event_id OR NOT(g.vehicle_id <=> o.vehicle_id)
            OR r.id IS NULL OR r.event_id<>o.event_id OR r.user_id<>o.user_id OR r.status<>'CONFIRMED'
            OR NOT EXISTS(SELECT 1 FROM event_registration_vehicles v WHERE v.registration_id=r.id AND v.vehicle_id=o.vehicle_id))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='BACK-537: selected ride differs from retained guest/registration; reconcile before migration';
    END IF;
END$$
DELIMITER ;
CALL back537_preflight();
DROP PROCEDURE back537_preflight;

CREATE TEMPORARY TABLE back537_guest_before AS
    SELECT id,event_id,status,vehicle_id FROM event_guest_requests;

ALTER TABLE event_guest_requests
    ADD COLUMN origin_registration_id BIGINT NULL,
    ADD COLUMN requested_vehicle_id BIGINT NULL,
    ADD COLUMN allocated_registration_id BIGINT NULL,
    ADD COLUMN responsible_user_id BIGINT NULL,
    ADD COLUMN selected_offer_id BIGINT NULL,
    MODIFY COLUMN rejection_reason VARCHAR(1000) NULL,
    DROP INDEX uk_event_guest_cpf,
    DROP INDEX uk_event_guest_vehicle,
    ADD INDEX idx_event_guest_cpf(event_id,cpf),
    ADD INDEX idx_event_guest_capacity(event_id,status,vehicle_id);

ALTER TABLE event_ride_offers ADD COLUMN withdrawn_at DATETIME(6) NULL,
    DROP INDEX uk_event_ride_response,
    ADD INDEX idx_event_ride_response(guest_request_id,vehicle_id,status);

UPDATE event_guest_requests g JOIN event_registrations r ON r.event_id=g.event_id AND r.user_id=g.requester_user_id
    SET g.origin_registration_id=r.id,g.requested_vehicle_id=g.vehicle_id WHERE g.administrative=b'0';

UPDATE event_guest_requests g JOIN event_registration_vehicles v ON v.vehicle_id=g.vehicle_id
    JOIN event_registrations r ON r.id=v.registration_id AND r.event_id=g.event_id AND r.status='CONFIRMED'
    SET g.allocated_registration_id=r.id,g.responsible_user_id=r.user_id WHERE g.status='APPROVED';

UPDATE event_guest_requests g JOIN event_ride_offers o ON o.guest_request_id=g.id AND o.status='SELECTED'
    SET g.selected_offer_id=o.id;

-- A pending vehicle was a preference, never an occupied seat. Preserve it separately.
UPDATE event_guest_requests SET vehicle_id=NULL,approved_vehicle_id=NULL WHERE status<>'APPROVED';
-- Explicit business reconciliation: ADMIN creation now means active participation.
-- Original reviewed_at/reviewer_id/created_at remain untouched. REJECTED stays REJECTED.
UPDATE event_guest_requests SET status='APPROVED' WHERE administrative=b'1' AND status='PENDING';

CREATE TABLE event_guest_transitions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    guest_request_id BIGINT NOT NULL,
    action VARCHAR(80) NOT NULL,
    actor_user_id BIGINT NULL,
    reason VARCHAR(1000) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    previous_status VARCHAR(30) NULL,
    next_status VARCHAR(30) NOT NULL,
    previous_vehicle_id BIGINT NULL,
    next_vehicle_id BIGINT NULL,
    previous_registration_id BIGINT NULL,
    next_registration_id BIGINT NULL,
    offer_id BIGINT NULL,
    INDEX idx_event_guest_transition(event_id,guest_request_id,id)
) ENGINE=InnoDB;

-- Migration snapshots describe reconciliation time, not invented human reviews.
INSERT INTO event_guest_transitions(event_id,guest_request_id,action,actor_user_id,reason,occurred_at,
    previous_status,next_status,previous_vehicle_id,next_vehicle_id,previous_registration_id,next_registration_id,offer_id)
SELECT g.event_id,g.id,'LEGACY_MIGRATION',NULL,
    'BACK-537 legacy snapshot; ADMIN pending becomes active standby; pending/rejected vehicle is retained as personal preference',
    UTC_TIMESTAMP(6),b.status,g.status,b.vehicle_id,g.vehicle_id,
    CASE WHEN b.status='APPROVED' THEN g.allocated_registration_id ELSE NULL END,g.allocated_registration_id,g.selected_offer_id
FROM event_guest_requests g JOIN back537_guest_before b ON b.id=g.id;
DROP TEMPORARY TABLE back537_guest_before;

ALTER TABLE event_registrations ADD CONSTRAINT uk_event_registration_identity UNIQUE(id,event_id,user_id);
ALTER TABLE event_guest_requests
    ADD CONSTRAINT fk_event_guest_origin FOREIGN KEY(origin_registration_id,event_id,requester_user_id)
        REFERENCES event_registrations(id,event_id,user_id),
    ADD CONSTRAINT fk_event_guest_transport FOREIGN KEY(allocated_registration_id,event_id,responsible_user_id)
        REFERENCES event_registrations(id,event_id,user_id),
    ADD CONSTRAINT fk_event_guest_selected_offer FOREIGN KEY(selected_offer_id) REFERENCES event_ride_offers(id),
    ADD CONSTRAINT ck_event_guest_origin CHECK (
        (administrative=b'1' AND origin_registration_id IS NULL AND requested_vehicle_id IS NULL AND status<>'PENDING') OR
        (administrative=b'0' AND origin_registration_id IS NOT NULL AND requested_vehicle_id IS NOT NULL)),
    ADD CONSTRAINT ck_event_guest_transport CHECK (
        (status='APPROVED' AND vehicle_id IS NOT NULL AND allocated_registration_id IS NOT NULL AND responsible_user_id IS NOT NULL) OR
        (vehicle_id IS NULL AND allocated_registration_id IS NULL AND responsible_user_id IS NULL AND selected_offer_id IS NULL
            AND (status<>'APPROVED' OR administrative=b'1'))),
    ADD CONSTRAINT ck_event_guest_status CHECK(status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    ADD CONSTRAINT ck_event_guest_legacy_projection CHECK(approved_vehicle_id <=> IF(status='APPROVED',vehicle_id,NULL));
ALTER TABLE event_ride_offers ADD CONSTRAINT ck_event_ride_withdrawal CHECK (
    (status='WITHDRAWN' AND withdrawn_at IS NOT NULL AND withdrawn_at>=COALESCE(selected_at,responded_at)) OR
    (status IN ('ACCEPTED','DECLINED','SELECTED') AND withdrawn_at IS NULL));

-- Permission ENUM extension follows; catalog bootstrap creates definitions and
-- ROOT structural access. No CUSTOM role is automatically granted new capabilities.
ALTER TABLE authorization_permissions MODIFY COLUMN code ENUM ('AUTHENTICATION_EXPORT','AUTHENTICATION_PASSWORD_RECOVERY_CANCEL','AUTHENTICATION_PASSWORD_RECOVERY_READ','AUTHENTICATION_REFRESH_TOKEN_READ','AUTHENTICATION_REFRESH_TOKEN_REVOKE','AUTHENTICATION_SESSION_LOGOUT','AUTHENTICATION_SESSION_READ','AUTHENTICATION_USER_PASSWORD_RESET_LINK_GENERATE','AUTHENTICATION_USER_TEMPORARY_PASSWORD_GENERATE','AUTHORIZATION_EXPORT','AUTHORIZATION_PERMISSION_ASSIGN','AUTHORIZATION_PERMISSION_READ','AUTHORIZATION_PERMISSION_REVOKE','AUTHORIZATION_ROLE_CREATE','AUTHORIZATION_ROLE_DELETE','AUTHORIZATION_ROLE_DISABLE','AUTHORIZATION_ROLE_ENABLE','AUTHORIZATION_ROLE_READ','AUTHORIZATION_ROLE_UPDATE','AUTHORIZATION_USER_ROLE_ASSIGN','AUTHORIZATION_USER_ROLE_READ','AUTHORIZATION_USER_ROLE_REVOKE','BILLING_CHARGE_ASSIGNMENT_CREATE','BILLING_CHARGE_ASSIGNMENT_READ','BILLING_CHARGE_ASSIGNMENT_UPDATE','BILLING_CHARGE_CYCLE_ARCHIVE','BILLING_CHARGE_CYCLE_CANCEL','BILLING_CHARGE_CYCLE_FINISH','BILLING_CHARGE_CYCLE_GENERATE','BILLING_CHARGE_CYCLE_READ','BILLING_CHARGE_DEFINITION_CREATE','BILLING_CHARGE_DEFINITION_READ','BILLING_CHARGE_DEFINITION_UPDATE','BILLING_EXPORT','BILLING_MEMBER_CHARGE_CANCEL','BILLING_MEMBER_CHARGE_READ','BILLING_MEMBER_CHARGE_UPDATE','BILLING_PAYMENT_CONFIRM','BILLING_PAYMENT_READ','BILLING_PAYMENT_REJECT','BILLING_REFUND_APPROVE','BILLING_REFUND_CANCEL','BILLING_REFUND_EXPIRE','BILLING_REFUND_MARK_AS_REFUNDED','BILLING_REFUND_READ','BILLING_REFUND_REJECT','DEPENDENTS_DEPENDENT_EXPORT','DEPENDENTS_DEPENDENT_READ','HEALTH_MEDICAL_PROFILE_DELETE','HEALTH_MEDICAL_PROFILE_EXPORT','HEALTH_MEDICAL_PROFILE_READ','HEALTH_MEDICAL_PROFILE_UPDATE','IDENTITY_USER_CREATE','IDENTITY_USER_DISABLE','IDENTITY_USER_ENABLE','IDENTITY_USER_EXPORT','IDENTITY_USER_READ','IDENTITY_USER_UPDATE','MEMBERSHIP_BILLING_CONFIGURATION_READ','MEMBERSHIP_BILLING_CONFIGURATION_UPDATE','MEMBERSHIP_EXPORT','MEMBERSHIP_MEMBERSHIP_APPLICANT_BLOCK','MEMBERSHIP_MEMBERSHIP_APPLICANT_UNBLOCK','MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE','MEMBERSHIP_MEMBERSHIP_REQUEST_INVITE_RESEND','MEMBERSHIP_MEMBERSHIP_REQUEST_READ','MEMBERSHIP_MEMBERSHIP_REQUEST_REJECT','PUBLICATIONS_COMMENT_CREATE','PUBLICATIONS_COMMENT_READ','PUBLICATIONS_EVENT_ADMIN_DASHBOARD_READ','PUBLICATIONS_EVENT_CANCEL','PUBLICATIONS_EVENT_CREATE','PUBLICATIONS_EVENT_DELETE','PUBLICATIONS_EVENT_FINISH','PUBLICATIONS_EVENT_GUEST_ADMIN_CREATE','PUBLICATIONS_EVENT_GUEST_ADMIN_READ','PUBLICATIONS_EVENT_GUEST_APPROVE','PUBLICATIONS_EVENT_GUEST_REJECT','PUBLICATIONS_EVENT_GUEST_REQUEST_CREATE','PUBLICATIONS_EVENT_GUEST_REQUEST_READ','PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ','PUBLICATIONS_EVENT_PUBLISH','PUBLICATIONS_EVENT_READ','PUBLICATIONS_EVENT_READ_ADMIN','PUBLICATIONS_EVENT_REGISTER','PUBLICATIONS_EVENT_REGISTRATION_CANCEL','PUBLICATIONS_EVENT_REGISTRATION_READ','PUBLICATIONS_EVENT_RIDE_OFFER_READ','PUBLICATIONS_EVENT_RIDE_OFFER_RESPOND','PUBLICATIONS_EVENT_RIDE_OFFER_SELECT','PUBLICATIONS_EVENT_UPDATE','PUBLICATIONS_EXPORT','PUBLICATIONS_FEED_READ','PUBLICATIONS_INTERACTION_LIKE','PUBLICATIONS_NOTICE_ARCHIVE','PUBLICATIONS_NOTICE_CREATE','PUBLICATIONS_NOTICE_DELETE','PUBLICATIONS_NOTICE_PUBLISH','PUBLICATIONS_NOTICE_READ','PUBLICATIONS_NOTICE_UPDATE','PUBLICATIONS_PUBLICATION_READ','PUBLICATIONS_SERVICE_ADMIN_DELETE','PUBLICATIONS_SERVICE_ADMIN_READ','PUBLICATIONS_SERVICE_CHANGE_REQUEST_ADMIN_READ','PUBLICATIONS_SERVICE_CHANGE_REQUEST_APPROVE','PUBLICATIONS_SERVICE_CHANGE_REQUEST_CREATE','PUBLICATIONS_SERVICE_CHANGE_REQUEST_READ','PUBLICATIONS_SERVICE_CHANGE_REQUEST_REJECT','PUBLICATIONS_SERVICE_DELETE','PUBLICATIONS_SERVICE_READ','PUBLICATIONS_SERVICE_REQUEST_ADMIN_READ','PUBLICATIONS_SERVICE_REQUEST_APPROVE','PUBLICATIONS_SERVICE_REQUEST_CREATE','PUBLICATIONS_SERVICE_REQUEST_READ','PUBLICATIONS_SERVICE_REQUEST_REJECT','TOOLS_TOOL_ACTIVATE','TOOLS_TOOL_CREATE','TOOLS_TOOL_DEACTIVATE','TOOLS_TOOL_DELETE','TOOLS_TOOL_EXPORT','TOOLS_TOOL_READ','TOOLS_TOOL_UPDATE','VEHICLES_VEHICLE_CREATE','VEHICLES_VEHICLE_DELETE','VEHICLES_VEHICLE_EXPORT','VEHICLES_VEHICLE_READ','VEHICLES_VEHICLE_UPDATE','MEMBERSHIP_MEMBER_REGISTRATION_MANAGE','MEMBERSHIP_MEMBER_READ','MEMBERSHIP_MEMBER_AUDIT_READ','MEMBERSHIP_MEMBER_NUMBER_RESERVE','MEMBERSHIP_MEMBER_NUMBER_ISSUE','MEMBERSHIP_MEMBER_IMPORT','IDENTITY_USER_ARCHIVE','IDENTITY_USER_UNARCHIVE','IDENTITY_USER_DELETE','IDENTITY_USER_HISTORY_READ','AUTHORIZATION_ROLE_ARCHIVE','AUTHORIZATION_ROLE_UNARCHIVE','AUTHORIZATION_ROLE_HISTORY_READ','DEPENDENTS_DEPENDENT_ACTIVATE','DEPENDENTS_DEPENDENT_DEACTIVATE','DEPENDENTS_DEPENDENT_ARCHIVE','DEPENDENTS_DEPENDENT_UNARCHIVE','DEPENDENTS_DEPENDENT_DELETE','TOOLS_TOOL_ARCHIVE','TOOLS_TOOL_UNARCHIVE','VEHICLES_VEHICLE_ACTIVATE','VEHICLES_VEHICLE_DEACTIVATE','VEHICLES_VEHICLE_ARCHIVE','VEHICLES_VEHICLE_UNARCHIVE','BILLING_CHARGE_DEFINITION_DELETE','BILLING_CHARGE_DEFINITION_HISTORY_READ','DEPENDENTS_DEPENDENT_PHOTO_UPDATE','PUBLICATIONS_EVENT_GUEST_REQUEST_CANCEL','PUBLICATIONS_EVENT_GUEST_ALLOCATE','PUBLICATIONS_EVENT_GUEST_UNASSIGN','PUBLICATIONS_EVENT_GUEST_CANCEL','PUBLICATIONS_EVENT_GUEST_REACTIVATE','PUBLICATIONS_EVENT_GUEST_HISTORY_READ') NOT NULL;
