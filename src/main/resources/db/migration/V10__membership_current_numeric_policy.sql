-- BACK-526 policy homologated 2026-10-26. Stop all application writers first.
-- Preflight precedes business writes. DML is atomic; MySQL DDL/history is not.
DELIMITER $$
CREATE PROCEDURE back526_current_numeric_policy()
BEGIN
    DECLARE coordinator BIGINT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    START TRANSACTION;
    SELECT id INTO coordinator FROM membership_registry_sequence WHERE id=1 FOR UPDATE;
    IF coordinator IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='BACK-526 preflight: registry coordinator missing';
    END IF;
    IF EXISTS (SELECT 1 FROM membership_members WHERE NOT REGEXP_LIKE(membership_number,'^[0-9]+$','c') OR REGEXP_LIKE(membership_number,'^0+$','c'))
       OR EXISTS (SELECT 1 FROM membership_number_claims WHERE member_id IS NULL AND
           (NOT REGEXP_LIKE(membership_number,'^[0-9]+$','c') OR REGEXP_LIKE(membership_number,'^0+$','c'))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='BACK-526 preflight: nonpositive or nonnumeric current number/reservation';
    END IF;
    IF EXISTS (
        SELECT canonical_number FROM (
            SELECT TRIM(LEADING '0' FROM membership_number) AS canonical_number FROM membership_members
            UNION ALL
            SELECT TRIM(LEADING '0' FROM membership_number) FROM membership_number_claims WHERE member_id IS NULL
        ) occupied GROUP BY canonical_number HAVING COUNT(*)>1
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='BACK-526 preflight: canonical number collision';
    END IF;
    IF EXISTS (SELECT 1 FROM membership_members m LEFT JOIN membership_number_claims c
               ON c.member_id=m.id AND BINARY c.membership_number=BINARY m.membership_number WHERE c.claim_key IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='BACK-526 preflight: current Member claim missing';
    END IF;
    -- Archive all claim metadata verbatim. Historical opaque literals are never converted.
    INSERT INTO membership_member_audits(member_id,previous_number,new_number,actor_user_id,occurred_at,reason,operation,outcome)
    SELECT member_id,NULL,membership_number,actor_user_id,claimed_at,reason,'LEGACY_CLAIM_SNAPSHOT','SUCCESS'
    FROM membership_number_claims;
    DELETE c FROM membership_number_claims c LEFT JOIN membership_members m
        ON c.member_id=m.id AND BINARY c.membership_number=BINARY m.membership_number
    WHERE c.member_id IS NOT NULL AND m.id IS NULL;
    UPDATE membership_members SET membership_number=TRIM(LEADING '0' FROM membership_number),
        version=version+1,updated_at=UTC_TIMESTAMP(6)
    WHERE membership_number<>TRIM(LEADING '0' FROM membership_number);
    UPDATE membership_number_claims SET membership_number=TRIM(LEADING '0' FROM membership_number),
        claim_key=SHA2(CONCAT('numeric:',TRIM(LEADING '0' FROM membership_number)),256);
    -- Preserve the row as mutex, never as the source of the next number.
    UPDATE membership_registry_sequence SET next_value=NULL,format_signature=NULL,version=version+1 WHERE id=1;
    COMMIT;
END$$
CALL back526_current_numeric_policy()$$
DROP PROCEDURE back526_current_numeric_policy$$
DELIMITER ;
