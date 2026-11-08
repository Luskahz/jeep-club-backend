# BACK-562 — Event HTTP coverage

Final PR #136 audit follow-up, 2026-11-08. Existing positive scenarios are reused rather than duplicated. `EventHttpIntegrationTest` also runs through the MySQL/PostgreSQL Testcontainers subclasses.

| Production method | Positive HTTP evidence | Relevant negative evidence |
| --- | --- | --- |
| `EventController.read` | `memberOperationsBindPrincipalAndPersistOnlyAuthorizedEffects` | `endpointPermissionsAreEnforcedIncludingEmergencyHealth` |
| `allocate` | Same test: principal and persisted allocation | Invalid allocation retains state; independent ownership/capacity integration tests |
| `cancel` | Same test: owner cancellation persists | Another principal receives 404 and cannot cancel the owner registration |
| `guests` | Same test and `memberGuestCancellationOwnershipAndDuplicateCpfContract` | Own-resource isolation and authentication/permission tests |
| `rides` | Same test: eligible standby guest | Service/database integration scenarios for pending, vehicleless, full and closed registrations |
| `respond` | Same test: accepted offer and administrative readback | `rejectedRideResponsesLeaveNoOffersOrFalseHistory` |

The new response-rejection test covers an unregistered principal (404), pending participation (409), another user's vehicle (409), invalid payload (400), insufficient permission (403), duplicate accepted offer (409), and a cancelled/unavailable guest (409). Each rejection checks Problem Details content type, title, detail, status and controlled code. It compares all persisted offers, guest transitions, guest states and registrations before/after the request, including rejection after an accepted offer already exists.

JaCoCo from the successful final focused `verify`: each of `allocate`, `read`, `cancel`, `guests`, `rides`, `respond` has METHOD covered=1/missed=0 and LINE covered=1/missed=0. These counters establish execution of all six methods, not exhaustive branch coverage.

Full local gates: `test` and `verify` each ran 2,590 tests (0 failures/errors; one existing Windows symbolic-link skip), MySQL 530 and PostgreSQL 146 (0 failures/errors/skips). After strengthening only test fixtures/transaction boundaries, all 64 affected HTTP/database tests were repeated successfully. See [PR #136 final evidence](pr-136-final-audit.md) for commands, transaction boundaries and SQL measurements. The historical final-hardening CSV remains an unchanged snapshot of the earlier audit.
