# BACK-597: pending security decision

Manual temporary passwords have 26^5 * 10^3 possible values (approximately 33.5 bits). They have no dedicated expiry in the current account model, including for a previously active account reset by an administrator. Five failed login attempts lock the account through the shared login flow. Administrative temporary password recovery revokes active sessions and refresh tokens, stores a hash, and requires a password change. These controls do not remove the risk of an indefinitely valid short password.

Before merge, security and product owners must choose either a short lifetime (for example 24 hours) with secure reissuance/recovery, or formally accept the residual risk. A persistent TTL would require a separately designed schema migration and a policy for existing accounts; neither is included here. This document records a pending decision, not risk acceptance.
