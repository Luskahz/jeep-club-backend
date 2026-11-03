# BACK-585 / BACK-574 / BACK-463 / BACK-573

## Audit before implementation

Baseline: origin/develop `f76541692c6c25bc082264bd63cc62ae9b365f48`.
Branch: `feature/back-585-574-463-573-admin-hardening`.
Jira full descriptions, acceptance criteria, links and comments inspected for all four stories,
their twelve actual subtasks, and related BACK-455/456/457/464. GitHub connector reports no
open PRs and no matching feature branch. PR #125 is merged into develop; its reported CI
is historical evidence only. Current workflow requires Maven verify and MySQL migration tests.
Local untracked output/ and tmp/ predate this task and are preserved.

BACK-463's real subtasks are BACK-473, BACK-474, BACK-475. BACK-464 is an unrelated,
completed Event/Billing cancellation task. BACK-455/456/457 were closed because they were
replaced by real subtasks, not because their acceptance criteria were implemented.

## Traceability and planned verification

| Story / subtask | Existing code | Necessary change | Verification |
| --- | --- | --- | --- |
| BACK-585 / BACK-586 | AdminUserJpaQueryRepository uses unescaped LIKE | Literal %, _, escape in every partial predicate | Persistence regression with literal metacharacters |
| BACK-585 / BACK-587 | UserSortMapper allowlist; no total order | Append id when absent, preserve explicit id direction | Sort unit tests and repeated-name pages |
| BACK-585 / BACK-588 | Tuple projection, Membership port, registry API tests | Strengthen combined filters, q OR, count, fields, later pages | Identity persistence/HTTP, registry integration, SQL inspection |
| BACK-574 / BACK-579 | name/status and full Tool summary | Add typed q/filter/fields contract with existing default | HTTP/OpenAPI/invalid parameters/security |
| BACK-574 / BACK-580 | ToolSpecifications/JpaSpecificationExecutor | Reuse predicates for count and selective DB projection | Persistence/count/pages/SQL columns |
| BACK-574 / BACK-581 | Tools contain owner userId; summary has no owner name | Assess need for owner search; bound integration if included | Compatibility and fixed query count |
| BACK-463 / BACK-473 | PublicationComment has no payload caps | Define technical text/image caps before external work | Exact limit, +1, empty, text/images, zero side effects |
| BACK-463 / BACK-474 | Event catches NPE/all DataAccessException; social labels all integrity failures as like conflict | Restrict functional conflicts to recognized failures | HTTP RFC 9457 for NPE/SQL/known constraints, no internal details |
| BACK-463 / BACK-475 | H2 and real MySQL suites, no PostgreSQL suite | Isolated PostgreSQL validation without production DB changes | Real PostgreSQL schema/constraints/concurrency/history/Billing |
| BACK-573 / BACK-576 | status/page/sort and batch owner presentation | Typed q/plate/ownerId/nickname/model/fields contract | HTTP/OpenAPI/security/normalization |
| BACK-573 / BACK-577 | Entity page then owner-name batch | DB predicates/count and selective projection, stable id | Persistence/count/later pages/selected columns |
| BACK-573 / BACK-578 | Public Identity names-by-IDs API | Minimal bounded name-search contract | Cardinality limit, no N+1, owner-name integration |

## Risks and boundaries

* Preserve default JSON fields/nulls, default status behavior and existing permissions.
* q OR and specific filters AND must share count predicates; never filter a page in memory.
* Owner names belong to Identity; no cross-module entity/repository imports or unbounded IDs.
* Membership projection remains batched; do not add per-row reads.
* Contains LIKE generally scans; no index/performance claim without representative measurement.
* Current production database is MySQL/Flyway, although BACK-475 still names PostgreSQL.
  PostgreSQL validation must be isolated and cannot establish production migration readiness.
* New payload limits apply to input; preserve readability of existing persisted comments.
* Unexpected infrastructure failures must remain sanitized server errors, not known conflicts.
* No merge, deploy, master modification, production data operation or automatic Jira closure.

## Execution evidence

Pending implementation and current-HEAD validation. No acceptance criterion is declared complete yet.
