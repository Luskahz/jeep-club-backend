-- Stop ALL writers and take a verified backup before running this migration.
-- No business rows are changed. MySQL DDL commits implicitly; see deployment runbook.
DELIMITER $$
DROP PROCEDURE IF EXISTS pr121_reference_preflight$$
CREATE PROCEDURE pr121_reference_preflight()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.key_column_usage k
        JOIN information_schema.referential_constraints r
          ON r.constraint_schema=k.constraint_schema AND r.constraint_name=k.constraint_name AND r.table_name=k.table_name
        WHERE k.constraint_schema=DATABASE()
          AND ((k.table_name='dependents_dependent' AND k.column_name='user_id')
            OR (k.table_name='dependents_dependent_history' AND k.column_name IN ('user_id','deleted_by_user_id'))
            OR (k.table_name IN ('medical_profiles','medical_profiles_history')
                AND k.column_name IN ('user_owner_id','dependent_owner_id','deleted_by_user_id')))
          AND (r.delete_rule NOT IN ('RESTRICT','NO ACTION') OR r.update_rule NOT IN ('RESTRICT','NO ACTION'))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PR121 preflight: existing nonrestrictive reference constraint';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()
        AND table_name IN ('identity_users','dependents_dependent','dependents_dependent_history',
                          'medical_profiles','medical_profiles_history') AND engine='InnoDB') <> 5 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PR121 preflight: expected five InnoDB tables';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()
        AND data_type='bigint' AND column_type NOT LIKE '%unsigned%' AND is_nullable='NO'
        AND ((table_name='identity_users' AND column_name='id')
          OR (table_name='dependents_dependent' AND column_name IN ('id','user_id'))
          OR (table_name='dependents_dependent_history' AND column_name IN ('user_id','deleted_by_user_id'))
          OR (table_name='medical_profiles' AND column_name='owner_id')
          OR (table_name='medical_profiles_history' AND column_name IN ('owner_id','deleted_by_user_id')))) <> 8 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PR121 preflight: incompatible signed BIGINT reference columns';
    END IF;
    IF EXISTS (SELECT 1 FROM dependents_dependent child LEFT JOIN identity_users parent
        ON parent.id=child.user_id WHERE parent.id IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PR121 preflight: orphan dependents_dependent.user_id';
    END IF;
    IF EXISTS (SELECT 1 FROM dependents_dependent_history child LEFT JOIN identity_users parent
        ON parent.id=child.user_id WHERE parent.id IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PR121 preflight: orphan dependents_dependent_history.user_id';
    END IF;
    IF EXISTS (SELECT 1 FROM dependents_dependent_history child LEFT JOIN identity_users parent
        ON parent.id=child.deleted_by_user_id WHERE parent.id IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PR121 preflight: orphan dependents_dependent_history.deleted_by_user_id';
    END IF;
    IF EXISTS (SELECT 1 FROM medical_profiles_history child LEFT JOIN identity_users parent
        ON parent.id=child.deleted_by_user_id WHERE parent.id IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PR121 preflight: orphan medical_profiles_history.deleted_by_user_id';
    END IF;
    IF EXISTS (SELECT 1 FROM medical_profiles WHERE owner_type NOT IN ('USER','DEPENDENT')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PR121 preflight: invalid medical_profiles.owner_type';
    END IF;
    IF EXISTS (SELECT 1 FROM medical_profiles child LEFT JOIN identity_users parent
        ON parent.id=child.owner_id WHERE child.owner_type='USER' AND parent.id IS NULL)
       OR EXISTS (SELECT 1 FROM medical_profiles child LEFT JOIN dependents_dependent parent
        ON parent.id=child.owner_id WHERE child.owner_type='DEPENDENT' AND parent.id IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PR121 preflight: orphan medical_profiles.owner';
    END IF;
    IF EXISTS (SELECT 1 FROM medical_profiles_history WHERE owner_type NOT IN ('USER','DEPENDENT')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PR121 preflight: invalid medical_profiles_history.owner_type';
    END IF;
    IF EXISTS (SELECT 1 FROM medical_profiles_history child LEFT JOIN identity_users parent
        ON parent.id=child.owner_id WHERE child.owner_type='USER' AND parent.id IS NULL)
       OR EXISTS (SELECT 1 FROM medical_profiles_history child LEFT JOIN dependents_dependent parent
        ON parent.id=child.owner_id WHERE child.owner_type='DEPENDENT' AND parent.id IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PR121 preflight: orphan medical_profiles_history.owner';
    END IF;
END$$
CALL pr121_reference_preflight()$$
DROP PROCEDURE pr121_reference_preflight$$
DELIMITER ;

ALTER TABLE medical_profiles
    ADD COLUMN user_owner_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN owner_type='USER' THEN owner_id ELSE NULL END) STORED,
    ADD COLUMN dependent_owner_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN owner_type='DEPENDENT' THEN owner_id ELSE NULL END) STORED,
    ADD CONSTRAINT ck_medical_profiles_owner_type CHECK (owner_type IN ('USER','DEPENDENT'));

ALTER TABLE medical_profiles_history
    ADD COLUMN user_owner_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN owner_type='USER' THEN owner_id ELSE NULL END) STORED,
    ADD COLUMN dependent_owner_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN owner_type='DEPENDENT' THEN owner_id ELSE NULL END) STORED,
    ADD CONSTRAINT ck_medical_profiles_history_owner_type CHECK (owner_type IN ('USER','DEPENDENT'));

-- Existing equivalent restrictive constraints are retained, regardless of their names.
DELIMITER $$
DROP PROCEDURE IF EXISTS pr121_add_restrict_fk$$
CREATE PROCEDURE pr121_add_restrict_fk(IN child_table VARCHAR(64), IN child_column VARCHAR(64),
    IN parent_table VARCHAR(64), IN fk_name VARCHAR(64))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.key_column_usage k
        JOIN information_schema.referential_constraints r
          ON r.constraint_schema=k.constraint_schema AND r.constraint_name=k.constraint_name
          AND r.table_name=k.table_name
        WHERE k.constraint_schema=DATABASE() AND k.table_name=child_table AND k.column_name=child_column
          AND k.referenced_table_schema=DATABASE() AND k.referenced_table_name=parent_table
          AND k.referenced_column_name='id' AND r.delete_rule IN ('RESTRICT','NO ACTION')
          AND r.update_rule IN ('RESTRICT','NO ACTION')
          AND (SELECT COUNT(*) FROM information_schema.key_column_usage parts
               WHERE parts.constraint_schema=k.constraint_schema AND parts.table_name=k.table_name
               AND parts.constraint_name=k.constraint_name)=1) THEN
        SET @pr121_ddl=CONCAT('ALTER TABLE `',child_table,'` ADD CONSTRAINT `',fk_name,
            '` FOREIGN KEY (`',child_column,'`) REFERENCES `',parent_table,
            '`(id) ON DELETE RESTRICT ON UPDATE RESTRICT');
        PREPARE pr121_stmt FROM @pr121_ddl;
        EXECUTE pr121_stmt;
        DEALLOCATE PREPARE pr121_stmt;
    END IF;
END$$
CALL pr121_add_restrict_fk('dependents_dependent','user_id','identity_users','fk_dependent_identity_user')$$
CALL pr121_add_restrict_fk('dependents_dependent_history','user_id','identity_users','fk_dependent_history_identity_user')$$
CALL pr121_add_restrict_fk('dependents_dependent_history','deleted_by_user_id','identity_users','fk_dependent_history_actor')$$
CALL pr121_add_restrict_fk('medical_profiles','user_owner_id','identity_users','fk_medical_profile_user_owner')$$
CALL pr121_add_restrict_fk('medical_profiles','dependent_owner_id','dependents_dependent','fk_medical_profile_dependent_owner')$$
CALL pr121_add_restrict_fk('medical_profiles_history','user_owner_id','identity_users','fk_medical_history_user_owner')$$
CALL pr121_add_restrict_fk('medical_profiles_history','dependent_owner_id','dependents_dependent','fk_medical_history_dependent_owner')$$
CALL pr121_add_restrict_fk('medical_profiles_history','deleted_by_user_id','identity_users','fk_medical_history_actor')$$
DROP PROCEDURE pr121_add_restrict_fk$$
DELIMITER ;

