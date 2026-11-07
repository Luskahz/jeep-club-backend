# BACK-562 — Event HTTP coverage

The historical final-hardening CSV remains the snapshot of the earlier audit. The focused HTTP run with JaCoCo, using `EventHttpIntegrationTest.memberOperationsBindPrincipalAndPersistOnlyAuthorizedEffects`, recorded METHOD 1/1 and LINE 1/1 for each of `EventController.allocate`, `read`, `cancel`, `guests`, `rides` and `respond`. These are method-level counters, not a claim of complete branch coverage.

| Production operation | HTTP evidence |
| --- | --- |
| `read` | `memberOperationsBindPrincipalAndPersistOnlyAuthorizedEffects` |
| `allocate` | same test: principal, binding, invalid allocation and retained state |
| `cancel` | same test: other principal cannot cancel, owner state persists |
| `guests` | same test: own empty page; `memberGuestCancellationOwnershipAndDuplicateCpfContract` covers ownership |
| `rides` | same test: eligible standby guest |
| `respond` | same test: accepted offer and administrative readback |

Existing `endpointPermissionsAreEnforcedIncludingEmergencyHealth` and `guestLifecyclePermissionsAreIndependentAndAllNewRoutesAreAuthenticated` cover authentication and permission failures. The historical CSV is not rewritten because it records a different code revision and test run.
