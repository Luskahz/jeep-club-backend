-- V13 belongs to Storage and must be applied before this migration in deployed environments.
-- MySQL's existing UNIQUE(name) depends on the column collation. The generated
-- value also enforces the invariant when that collation is case-sensitive.
ALTER TABLE billing_charge_definitions
    ADD COLUMN name_folded varchar(120) GENERATED ALWAYS AS (lower(name)) STORED,
    ADD CONSTRAINT uk_billing_charge_definitions_name_folded UNIQUE (name_folded);
