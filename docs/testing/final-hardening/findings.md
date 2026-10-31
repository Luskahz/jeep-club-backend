# Findings and decisions

These are test-quality findings unless explicitly identified as a production defect. No exploitable production bug was demonstrated; no production Java source or migration is changed.

| ID | Severity | Cause and impact | Reproduction/evidence | Resolution and residual |
| --- | --- | --- | --- | --- |
| F01 | HIGH | Administrative Authorization guards had little direct behavioral coverage. A removed ROOT/authority/binding rule could evade the old service/controller tests. | Baseline focal PIT 71/160 killed; traceability rows for AdminUserRoleService/AdminRolePermissionService. | Direct decision tests, actual JWT/Method Security/HTTP persistence, same-token revocation and two MySQL parent-lock disputes. Final mutation decisions distinguish unreachable history and unused return values from security guards. No production privilege bypass was found. |
| F02 | HIGH | Identity line coverage masked mutation-time, archival reconstitution and normalization decisions. | Baseline User/UserQueryService PIT 92/137 killed and 29 NO_COVERAGE. | UserMutationBoundaryTest and UserQueryServiceTest plus real adapter/filter cases. Redundant guards are individually justified. |
| F03 | HIGH | Event report mocks returned every known identity/vehicle regardless of the public query's requested IDs. Broad contains assertions could miss lost reference composition, cross-household summaries and incorrect capacity cells. | Baseline Event focal PIT 161/290 killed. | Fixtures now honor IDs; report cells, sparse requester/responsible/current owner, financial household isolation, health privacy, filters and capacity are asserted. No new financial or lifecycle rule is introduced. |
| F07 | HIGH | RoleDeletionHistoryService had tests but the separate historical HTTP authority/no-store/audit boundary had no execution evidence. | First clean final XML: RoleDeletionHistoryController CLASS missed. | Two real HTTP cases cover the distinct authority, snapshot contract, durable audit and 400/404. No production access bypass was found. The first full pipeline remains intermediate; a fresh final pipeline validates the addition. |
| F04 | MEDIUM | Six MySQL IT classes cached live Spring/Hikari contexts after their class-owned MySQL container had stopped. Pool adders attempted reconnects to dead mapped ports. | Baseline MySQL log warnings after completed classes; classes identified in platform-shared.md. | AFTER_CLASS DirtiesContext closes pools before container teardown. Registry already had closure and is left unchanged. Final MySQL/CI must prove cleanup. This was a test-resource defect, not a production connection failure. |
| F05 | MEDIUM | Logging tests did not distinguish dropping only new overflow from losing pending events, or persistence failure from success. | Focal baseline queue/persistence mutants; real queue regression. | Full real queue, ordered drains, captured emitted WARN/INFO and explicit failure/requeue/next worker pass. No scheduler sleeps or arbitrary retries. |
| F06 | LOW | A mutation changes filename extension parsing only for a basename starting with a dot, such as .png. | ImageMediaService.store:32 conditional-boundary mutant. | Accepted narrow filename-metadata edge: canonical MIME/signature/size/private namespace/storage key remain independently tested. No artificial filename assertion is added solely for score; original behavior is preserved. |

No CRITICAL finding was identified. The presence of a high-risk historical test gap does not establish an existing production vulnerability.

## Development diagnostics retained

Failed development executions are preserved outside target and never counted as acceptance. A compile-only attempt caught duplicate after-class annotation on Registry (already present); it was removed before the passing compile. Publications focused attempts 2/3 failed on Mockito re-stubbing answers receiving matcher null; switching those stubs to doReturn/doAnswer fixed the fixture. An intermediate sparse-guest fixture lacked mandatory origin/allocation registration IDs; the domain correctly rejected it, and the fixture was corrected. Production constructors and assertions were not weakened. Latest focused and final full results supersede these attempts.

The first Authorization PIT re-run left a save-removal mutant because verification occurred only after two transitions; immediate per-transition assertions fixed this detection gap. Event and Platform PIT iterations likewise drove explicit semantic assertions rather than internal-spy calls.

## CI execution budget

F08 — MEDIUM test-infrastructure finding: initial PR run 41251064283 passed Maven (2513/0/0/0) and the main MySQL suite (477/0/0/0), then the existing full-suite archive/next-migration probe hit the job's 20-minute ceiling. Check annotation: "The job has exceeded the maximum execution time of 20m0s". The main MySQL run alone ended approximately 14 minutes after job start; two full runs require a larger total budget. Only that job ceiling becomes 40 minutes. No test timeout, assertion, migration, skip policy or retry is relaxed. A new exact-head CI run must pass both full executions; the cancelled run is not a green gate.

The role-history MySQL fixture refinement also retained the original assertion after discovering its outer REPEATABLE_READ snapshot could not see the already committed REQUIRES_NEW audit. Disabling the enclosing test transaction exercises actual HTTP/service transaction ownership; the corrected 13-case MySQL selection passes. This is an integration-fixture isolation correction, not a production audit change.

Existing GitHub action-version deprecation/runner-image notices are platform warnings outside this behavioral scope; no action-version migration is bundled here.
