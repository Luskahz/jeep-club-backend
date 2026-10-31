# PR #121: retained principal references

V12 adds eight restrictive foreign keys, all with `ON DELETE RESTRICT ON UPDATE RESTRICT`:

| Child | Column | Parent |
| --- | --- | --- |
| dependents_dependent | user_id | identity_users.id |
| dependents_dependent_history | user_id | identity_users.id |
| dependents_dependent_history | deleted_by_user_id | identity_users.id |
| medical_profiles | user_owner_id (stored generated) | identity_users.id |
| medical_profiles | dependent_owner_id (stored generated) | dependents_dependent.id |
| medical_profiles_history | user_owner_id (stored generated) | identity_users.id |
| medical_profiles_history | dependent_owner_id (stored generated) | dependents_dependent.id |
| medical_profiles_history | deleted_by_user_id | identity_users.id |

Health retains its public `(owner_type, owner_id)` association and unique owner index.
Each generated column projects only its matching owner type; the other is NULL.
CHECK constraints reject an invalid ENUM sentinel even in a non-strict SQL session.
These columns are database implementation details, absent from JPA and DTOs.
Hibernate `validate` permits these extra physical columns. No cross-context JPA
relationships, triggers, automatic deletion or nullification are introduced.

The MySQL 8.0 documentation permits restrictive FKs on stored generated columns:
https://dev.mysql.com/doc/refman/8.0/en/create-table-generated-columns.html
The required evidence is the MySQL 8.0.46 integration suite, including mutations of
the base discriminator and owner ID, not merely acceptance of the DDL.

## Retention and deletion routes

The existing guards retain both owner and deletion actor references. Historical
`dependent_id` and `medical_profile_id` are snapshots of removed records and must
NOT reference the operational tables. Medical history continues to reference a
live (possibly inactive/archived) owner. Dependent history retains its Identity
owner and actor after physical dependent deletion. No data is reassociated or
renumbered. Inactive and archived owners remain valid references.

UserAdministrationService, DependentService (including admin delegation), and
personal/admin medical profile services remain the application deletion entry
points. Their guards remain in place. The repository adapters write deletion
history and remove the operational row in the same transaction.

## Lock order

Dependent creation already used `UserIdentityLock.byId()` via the Identity adapter
for the active check. The existence check now also uses that public lock, under
MANDATORY propagation. Existence, active check and persistence share the service
transaction; a concurrent user deletion cannot slip between validation and save.

Before deleting a dependent, lock owner and actor Identity rows in ascending ID
order, then the dependent, then its references/history. Before deleting a medical
profile, lock actor and USER owner Identity rows in ascending order, then the
DEPENDENT owner if applicable, then the profile. Delete locks do not require an
active owner, preserving administrative cleanup. Upserts keep their existing
owner-before-profile lock. These orders avoid new inversions caused by history
FK checks acquiring shared Identity locks after a child lock. Ordinary arbitrary
multi-statement SQL transactions may still deadlock and need transaction retry;
the FKs prevent orphan commits regardless.

## Production rollout and failure recovery

No production operation is performed by this change. Schedule a maintenance
window, verify a restorable backup, and stop **all** writers, including manual SQL,
jobs and old application instances. Run the read-only orphan joins matching the
V12 preflight beforehand to estimate remediation needs. Never disable FK checks.
The migration preflight checks five InnoDB tables, signed non-null BIGINT reference
types, valid owner discriminators and every owner/actor reference before any
business table DDL. Existing equivalent single-column restrictive FKs are reused.
MySQL validates indexes and references again during FK creation; indexes are reused
or created by InnoDB. Existing permissive constraints must be reviewed separately.

Adding STORED columns rebuilds the Health tables; FK validation scans child tables.
Expect metadata locks, write blocking, additional disk/redo usage and potentially
long DDL on large histories. Measure on a representative restored copy, check free
disk and monitor metadata lock waiters. Do not promise zero downtime or a fixed
duration from an empty database test.

A preflight failure raises SQLSTATE 45000 with the offending relationship, without
changing business rows. Investigate the actual records and obtain an explicit
data remediation decision; do not delete or invent parent rows automatically.
Flyway may record a failed migration and retain the helper procedure. After the
data issue is resolved and absence of partial table DDL is confirmed, an operator
may repair the failed Flyway entry and retry; helper procedures are recreated.

MySQL DDL is not transactionally atomic across tables. If failure occurs after
preflight, do not blindly repair/retry: capture SHOW CREATE TABLE and Flyway
history, compare each generated column/check/FK with V12, and restore the tested
backup or prepare a reviewed recovery script to reconcile only the applied DDL.
V12 is an incremental migration, not an idempotent schema repair tool. Keep
writers stopped until every intended constraint is verified in INFORMATION_SCHEMA
and application `ddl-auto=validate` succeeds. Never modify previously applied SQL.

## Evidence

Run `mvnw test`, `mvnw verify`, and
`mvnw -Pmysql-migrations -DskipUnitTests=true verify`.
MySqlMigrationIT checks empty and frozen V1/V2/V3 historical schemas, Hibernate
validation, data preservation, equivalent existing FKs, preflight failures and
physical enforcement. PrincipalLifecycleMySqlIT exercises real service transactions
in both race orders, simultaneous deletions, rollback and retained clinical history
using latches, without Thread.sleep. Existing Health HTTP MySQL tests cover the
administrative selectors and authorized clinical routes from PR #121.

Passing mock/H2 tests alone is insufficient for GO. Record actual results and CI
commit SHA in the PR; do not report unexecuted MySQL tests as successful.
