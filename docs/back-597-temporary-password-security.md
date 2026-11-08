# BACK-597 — manual temporary password validity

New manually delivered passwords retain exactly `^[a-z]{5}[0-9]{3}$` and expire 24 hours after server-side issuance. The Authentication account stores only the password hash and `manual_password_expires_at`; the password remains a one-time response with `Cache-Control: no-store`. No password or challenge is added to logs.

## Scope

The rule covers administrative Identity creation with `TEMPORARY_PASSWORD`, Membership temporary-password approval, and administrative password recovery. Explicit manual provisioning operations carry the distinction across Identity interfaces; password shape is never used to infer issuance type. Permanent passwords and strong internal credentials for `EMAIL_INVITATION` retain their existing policies. Invitation/reset tokens and links retain their own lifetimes.

Login rejects a manual credential when server time is equal to or later than its expiry, using the existing generic invalid-credentials response. No new challenge is issued. Required password completion validates both challenge and account expiry after Identity/account/challenge locks, then again after recovery/revocation locks. Expiry uses injected `Clock`, never a client timestamp. Unlocking after five failures leaves the deadline intact.

Reissue serializes on Identity then Authentication account, renews the deadline to a full 24 hours, replaces the hash, revokes sessions and refresh tokens, invalidates active password-change challenges and any previous activation invitation, and renews the recovery request to the same deadline. It preserves `PENDING_FIRST_ACCESS`, allowing Identity and approved Membership onboarding to finish through their existing completion boundary. A successful permanent-password change clears the manual deadline. Challenges remain single use. Failures roll back credential, challenge, session and onboarding mutations together.

## V15 and legacy policy

`V15__manual_password_expiration.sql` adds one nullable `DATETIME(6)` column without changing historical migrations or updating existing rows. V14 was the latest migration in the integrated develop and feature branches when V15 was selected.

Existing accounts keep NULL, including previously delivered manual credentials and strong internal invitation credentials. Hashes cannot identify the original delivery method reliably; no inference or blanket expiration is performed. NULL preserves historical behavior until a new manual issue sets the deadline. This compatibility exception is intentional: this migration does not retroactively impose a deadline on legacy manual passwords. Administrative reissue moves such an account onto the new rule.

## Verification

`ManualPasswordExpirationIntegrationTest` covers persisted deadlines, before/exact/after expiry, expired challenge completion, reissue, old credential/challenge rejection, five failures/unlock, session/refresh revocation, request renewal, legacy NULL, first access, and rollback. The same contract runs with Flyway and real MySQL in `ManualPasswordExpirationMySqlIT`, with additional contention tests requiring `performance_schema.data_lock_waits` before advancing time or reissuing. `MySqlMigrationIT` verifies V14-to-current preservation of legacy manual and strong credential hashes and NULL deadlines, alongside the complete historical-upgrade/next-migration suite. Existing Identity/Membership activation and recovery HTTP tests retain actual workflow coverage.

Security scope remains limited to the authorized short password format and 24-hour lifetime; five-attempt locking and single-use completion remain necessary controls.
