# Identity — BACK-435 / BACK-491 / BACK-492 / BACK-493

Baseline production/test bytecode: develop `f52e152363151c5556dd5f04e9dc7435e02ffecd`. Physical Surefire baseline: LINE 952 covered / 68 missed (93.3333%); BRANCH 502 / 83 (85.8120%). Historical Jira: 91.25% / 75.35%. See the consolidated report for all five counters and MySQL aggregation.

## Existing evidence retained

User domain normalization, atomic invalid profile updates, registration, /me, admin provisioning, lifecycle, uniqueness, private export, OpenAPI and RFC 9457 already have direct assertions. Registration/provisioning transactions and concurrency are covered by `UserRegistrationTransactionTest`, `AdminUserCreationIntegrationTest`, `UserAdministrationTransactionTest`, `UserProfileConcurrencyIntegrationTest`, `PrincipalLifecycleIntegrationTest` and its MySQL subclass. The matrix records concrete examples. These flows are not recreated.

The audit confirms Identity administrative activity and Authentication credential readiness are different concepts: pending-first-access Identity may be ACTIVE. Public UserQuery results must preserve INACTIVE/ARCHIVED state and absence, rather than infer Membership eligibility.

## Residual decisions addressed

- `UserQueryServiceTest`: null/empty input without persistence calls; inclusive 500-ID and keyset limits; invalid parameters before I/O; absent users; canonical uniqueness queries; all three administrative states and optional details through the public projection.
- `UserRepositoryAdapterTest`: real H2 keyset/batch results containing inactive/archived users; active-only projection; missing and formatted CPF with locking reads; null and self-exclusion in uniqueness queries.
- `UserRepositoryExceptionTranslationTest`: unrelated/unnamed persistence failures propagate unchanged. Existing H2 constraints already prove the positive unique-conflict translation.
- `UserContextBoundaryIntegrationTest`: real administrative state filtering with sparse fields and inclusive creation-time bounds. Existing combined filter/search/pagination/sort test remains.

No production rule, public contract or migration is changed by this stage.

The initial focal PIT generated 137 mutants: 92 KILLED, 16 SURVIVED and 29 NO_COVERAGE (67.1533% detected). This exposed residual User decisions despite its strong existing suite: mutation-time guards, archived profile/photo edits, inconsistent archival reconstitution, normalization length limits and the CPF zero-check-digit boundary. UserMutationBoundaryTest targets those decisions. The direct time validation in updateAdministratively is potentially redundant because updateProfile validates the same instant before any field mutation; equivalent survivors require an explicit final decision rather than artificial tests.

## Mutation scope and acceptance

`identity-hardening-mutation` selects User and UserQueryService, with existing domain/lifecycle/boundary tests and the new direct query tests. Domain lifecycle tests outside the Identity test package are explicitly included. This is an intentional subset; JaCoCo/full regressions cover the remaining surface.

## Stage validation

Focused Maven run: **91 cases, 0 failures, 0 errors, 0 skips**. This stage adds **49 discovered cases** (parameter expansions included), in three new test files and two extended existing files. Repeated execution in verify/PIT is not counted as additional scenarios.

Stage JaCoCo (baseline Surefire exec plus this focused run; same production bytecode): LINE **962 covered / 58 missed, 94.3137%** (+10 covered lines); BRANCH **538 / 47, 91.9658%** (+36 covered branches). Final full-suite and merged MySQL metrics remain a separate gate.

Focal PIT after the regression cases: **137 total, 135 KILLED, 2 SURVIVED, 0 NO_COVERAGE, 0 TIMED_OUT, 0 NON_VIABLE, 0 execution errors; 98.5401% detected**. The run completed in 58.435 seconds, using the same target classes as baseline. Both remaining mutants were reviewed:

| Location | Mutation | Decision |
| --- | --- | --- |
| User.normalizeRequiredDigits:352 | Remove validateLength | Equivalent for its only caller normalizeCpf: the immediately following exact-11-digit check still rejects every oversized value with IllegalArgumentException. Error-message wording is not the localized HTTP contract. Boundary cases are tested. |
| User.updateAdministratively:197 | Remove validateMutationInstant | Equivalent: updateProfile validates the identical instant before any field changes; CPF/email assignments happen only after it succeeds. No reflection or internal-spy test is added to distinguish redundant guards. |

Raw stage evidence is preserved locally under `identity-stage/` (JaCoCo XML/CSV, PIT XML/JSON and logs). Final gate results are recorded in the acceptance section below; formal review/integration remains outstanding. BACK-494 remains completed by scope absorption. Story completion additionally requires PR review and integration in develop.

## Final acceptance evidence

Measured clean implementation: 56cce5b41fe5682a714fd96e39a8124bf9dd7475. Full test/verify/MySQL gates and all four focal PIT goals exited zero. CI at the same implementation passed Maven, 479-case MySQL and the 479-case archive/forward probe; deploy was skipped. The delivery revision is the PR head containing this document; its exact SHA and fresh checks are recorded in PR/Jira after documentation publication.

Identity merged LINE: 962 covered / 58 missed (94.3137%); BRANCH: 538 / 47 (91.9658%).

Final focal PIT: 137 total; 135 KILLED, 2 SURVIVED, 0 NO_COVERAGE; zero timeout/nonviable/execution errors; 98.5401%. Individual decisions and exact killing tests are in mutation-decisions.csv and mutation-inventory.csv.

The matrix distinguishes concrete behavior/decision/contract evidence from execution coverage. No production Java, migration, data fixture, contract or coverage threshold changes. Residual subtasks are technically evidenced and remain in progress pending PR review/integration; absorbed subtasks remain done. See coverage-summary.md, mutation-summary.md and execution-evidence.json for physical counters, runner separation, commands, hashes and all outcomes.
