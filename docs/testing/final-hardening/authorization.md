# Authorization — BACK-436 / BACK-495–498

Surefire baseline: LINE 638 covered / 253 missed (71.6049%); BRANCH 141 / 121 (53.8168%). Historical Jira: 60.76% / 43.40%. The final comparison uses matching suite scope and physical counters.

## Audit findings and behavioral scope

The baseline executes some role creation, assignment, archival and deletion through lifecycle integrations. It does not execute `AdminUserRoleService.revokeRoleFromUser` or `replaceUserRoles`, nor `AdminRolePermissionService.removePermissionFromRole`. `AdminRoleService.updateRole` and deactivation also have no baseline execution. Controller characterization uses mocked services and annotation reflection; it is contract evidence, not proof that these business rules or Method Security work.

Existing `AuthorizationRoleQueryServiceTest`, `PermissionSynchronizationServiceTest`, `CurrentAuthorizationControllerTest`, OpenAPI tests, `PrincipalLifecycleIntegrationTest` and export tests remain useful and are reused. Current RolePresentationQueryJpaQuery already has historical-path execution; the old Jira statement that it was entirely uncovered is stale. Its current/mixed result branches still need direct behavioral evidence.

The critical residual scope is:

- real assign/revoke/replace semantics, duplicate input, missing Identity/Role, inactive/archived roles and preserving existing ROOT bindings;
- rejection of manual ROOT assignment/revocation and permission changes;
- normalized role conflicts, no-op saves/audits, retained-reference deletion guards and mandatory audit failure propagation;
- Permission synchronization, code mismatch and independent description/module changes;
- actual HTTP Method Security, RFC 9457 failures and replacement requiring both ASSIGN and REVOKE;
- removal of effective authorities after permission revocation, role deactivation or user-role revocation with an existing token;
- current/deleted/unknown role presentation through real queries;
- concurrent duplicate UserRole and RolePermission assignment, observing MySQL parent-lock contention and one final binding.

Privilege policy is the current controller authority policy plus ROOT protection. The code does not define a rule that administrators may only assign custom permissions they personally hold; no new subset policy is introduced by these tests. A read-only or inadequately authorized role must still be denied at the real HTTP boundary.

## Evidence and acceptance

The traceability matrix distinguishes existing evidence from planned residual cases. Unit service tests mock repository/public Identity ports and retain real Role/Permission objects. HTTP/persistence tests use real JWT/session validation and provision Identity through UserRegistration. MySQL tests use migrated MySQL 8.0.46, independent transactions, latches and live lock-wait observation.

The focal PIT profile selects Role, Permission and all three administrative services, and includes preexisting lifecycle tests outside the Authorization package. No DTO or Spring configuration mutation campaign is introduced.

## Stage validation

- Focused Maven: **74 cases, 0 failures/errors/skips**. This stage adds 68 Surefire-discovered cases (parameter expansions included); the six preexisting cases in this focused selection are not counted as new.
- Migrated MySQL: **11 cases, 0 failures/errors/skips**. Nine repeat the new HTTP/persistence scenarios; two are additional MySQL contention cases. Both require an actual waiter on the held parent row and verify exactly one final binding plus the precise duplicate-conflict exception.
- Initial PIT: **160 total, 71 KILLED, 28 SURVIVED, 61 NO_COVERAGE; 44.375%**.
- First hardening PIT: **152 KILLED, 5 SURVIVED, 3 NO_COVERAGE**. Analysis exposed a batched Mockito verification that could accept persistence on the wrong transition. The assertion now verifies the first save before the repeated no-op; missing-id actor overloads were also added.
- Final stage PIT: **160 total, 157 KILLED, 3 SURVIVED, 0 NO_COVERAGE, 0 TIMED_OUT/NON_VIABLE/errors; 98.125%**, 2m55s. The new real AuthorizationAdministrationIntegrationTest is explicitly included in the focal test filter; it did not exist in baseline.
- Stage JaCoCo before the last assertion refinements: LINE **822 covered / 69 missed, 92.2559%**; BRANCH **203 / 59, 77.4809%**. Final full-suite counters supersede this intermediate snapshot.

All remaining mutants were reviewed individually:

| Location | Mutation | Decision |
| --- | --- | --- |
| Role.ensureActive:233 | Remove ensureNotDeleted | Unreachable deleted operational Role: constructor/reconstitute reject deletedAt; deleted roles live in segregated history. Existing rejection test is retained; no reflection-created invalid instance. |
| Role.ensureCustom:226 | Remove ensureNotDeleted | Same constructor invariant. ROOT and archived restrictions remain independently tested and their relevant mutants are killed. |
| AdminRoleService.deleteRole:146 | Return null | The only production caller ignores this return and emits HTTP 204. Deletion/history, retained-reference guards and mandatory audit failures are asserted. No response body or state changes under this mutant. |

Raw stage evidence is in `authorization-stage/`. Final full regression, combined counters and exact-HEAD CI remain pending. Formal closure awaits review/integration; no attribution/revocation/ROOT guard survivor is being accepted as an untested security rule.

## Final XML audit refinement

The first clean full regression exposed RoleDeletionHistoryController as unexecuted. Its underlying snapshot service was covered by PrincipalLifecycleIntegrationTest, but that did not prove the route's distinct AUTHORIZATION_ROLE_HISTORY_READ authority, no-store response, RFC failures or HTTP-triggered durable audit. Two focused cases now exercise anonymous/read-only denial, authorized preserved permissions/metadata, no credential leakage, one successful audit, missing history and nonpositive ID without a success audit. These run through the real security chain and are inherited by MySQL. This adds two scenario inputs, not a duplicate full role lifecycle.

The earlier 68 new Authorization Surefire cases become 70; the nine inherited HTTP/query cases become eleven (the two independent MySQL contention cases remain). The first final pipeline is intermediate evidence at 38fcb577; a new clean final pipeline and exact-head CI must validate this refinement. No production source or migration changes.

The two history cases execute without an enclosing test transaction. The initial MySQL attempt caught an assertion reading a stale REPEATABLE_READ test snapshot after REQUIRES_NEW audit committed; the assertion remains exactly one durable row, now observed through real request-owned transactions. Corrected focused MySQL: **13 cases, 0 failures/errors/skips**. A separate earlier PowerShell -Dit.test quoting error executed no tests and is excluded from acceptance.
