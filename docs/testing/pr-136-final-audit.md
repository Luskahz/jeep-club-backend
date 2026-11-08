# PR #136 — Final audit evidence

2026-11-08. Branch: `feature/back-562-590-598-consolidated`; base: `develop`. The existing worktree and published commits were preserved. No other branch was merged or cherry-picked.

| Scope | Result and evidence |
| --- | --- |
| BACK-562 | Six controller methods remain covered; negative ride responses prove no offer, registration, guest-state or historical-transition changes. See [production-to-test matrix](back-562-event-http-coverage.md). |
| BACK-590 F01 | `participants()` keeps `readOnly=true` and uses nonlocking `PublicationRepository.findById()`. Missing resources, drafts and published non-Event subtypes return 404. Mutation locking remains unchanged. |
| BACK-590 F02 | Authentication plus controller-level `@RequiresMembership`; no granular read authority for this GET. HTTP covers anonymous 401, authenticated/no-authority 200 and Membership refusal 402. OpenAPI no longer advertises a granular permission and documents 402. |
| BACK-590 F03 | Explicit mapping to HTTP-owned `EventParticipantResponseDTO`, containing exactly `userId`, `name`, `profilePhotoStorageKey`; pagination metadata remains unchanged. OpenAPI and HTTP contracts are checked. |
| BACK-590 queries | Real database SQL pagination, status exclusions, deterministic order, grouped feed counts, zero detail count and bounded Identity lookups, measured below. |
| BACK-598 | Committed CREATE/UPDATE, direct JDBC name checks, detail/list reads, exact CSV/PDF spelling, controlled case-insensitive conflict and own-record update. Existing V14 and concurrent uniqueness tests are preserved. |

## Local execution

Java 17, Maven wrapper; all commands completed successfully. No production code changed between the full gates and the last focused run. The final focused run repeats every HTTP/database class affected by the subsequent stronger test fixtures and independently committed Billing requests. No source/test edits overlapped any run used as evidence.

| Command | Tests | Failures / errors / skipped |
| --- | ---: | --- |
| `./mvnw test` | 2590 | 0 / 0 / 1 |
| `./mvnw verify` | 2590 | 0 / 0 / 1 |
| `./mvnw -Pmysql-migrations -DskipUnitTests=true verify` | 530 | 0 / 0 / 0 |
| `./mvnw -Ppublications-postgresql -DskipUnitTests=true verify` | 146 | 0 / 0 / 0 |
| `./mvnw -Dtest=EventHttpIntegrationTest,BillingHttpWorkflowIntegrationTest,EventParticipantHttpMySqlIT,EventParticipantHttpPostgresIT verify` | 64 | 0 / 0 / 0 |

The existing local skip concerns Windows symbolic-link support. The MySQL profile includes 46 migration tests and 91 Event operations tests. PostgreSQL includes 85 Event operations tests. Billing concurrent uniqueness/V14 suites run four tests in each database. CI on the published SHA is tracked in the PR body and workflow artifacts, rather than a self-referential SHA inside this document.

## Real database HTTP and locking

`EventParticipantHttpMySqlIT` and `EventParticipantHttpPostgresIT` reuse the Spring application/MockMvc HTTP contracts against Testcontainers MySQL 8.0.46 and PostgreSQL 17.6. Authentication/Membership and vehicle boundary fixtures are simulated. The new database contract methods have **no outer test transaction**.

`publishedParticipantsReadThroughAConcurrentWriterWithoutTakingLocks` acquires `SELECT ... FROM publications WHERE id=? FOR UPDATE` on a separate JDBC connection. While that transaction still holds the row lock, another thread completes the HTTP request with 200. Only afterward does the test release the writer. Hibernate SQL capture asserts no locking SELECT, the repository spy verifies ordinary `findById`, and no vehicle lifecycle lock is invoked.

Existing real-contention write tests remain active: `confirmationTimestampStartsAfterActualCycleLockWait` observes `performance_schema.data_lock_waits` / `pg_blocking_pids`; MySQL additionally tests stale snapshots including `staleGuestSnapshotCannotReserveTheLastSeatTwice`. Both complete Event operation suites pass. No write transaction, mutation lock, repository lock implementation or lifecycle lock was relaxed.

## SQL and Identity measurements

`sqlPaginationAndIdentityBatchesHaveBoundedQueryCounts` seeds 502 confirmed holders plus pending/cancelled registrations. Identity calls delegate to the real `UserQueryService` and real `UserRepository` inside the participant service transaction; captured SQL therefore includes real Identity reads.

| Measurement | MySQL | PostgreSQL |
| --- | ---: | ---: |
| HTTP page size 1, total SQL | 5 | 5 |
| HTTP page size 20, total SQL | 5 | 5 |
| Service page size 501, Identity batch sizes | 500 + 1 | 500 + 1 |
| Identity SQL for that service page | 2 `IN` queries | 2 `IN` queries |
| Feed registration-count SQL across multiple events | 1 grouped query | 1 grouped query |

The HTTP page-size cap remains 50; the 501-item service test checks batching without changing that public limit. Participant SQL projects `user_id`, orders by registration `id`, and uses MySQL `limit ?,?` or PostgreSQL `offset ? rows fetch first ? rows only`. No registration occupant, vehicle or unallocated-dependent collection is loaded. Pending/cancelled holders are excluded; adjacent pages retain deterministic order. Detail counts match confirmed holders and become zero after cancellation. Existing HTTP feed assertions and the grouped-count unit test additionally cover correct counts and the zero default.

The `SqlCapture` inspector normalizes captured text to lowercase for assertions, returns the original SQL unchanged, and records placeholder-only statements in test output. Reports contain `BACK-590 ... SQL page=...`, `feed registration SQL`, `lock evidence` and the measured query-count line. The aggregate remains one `select event_id,count(id) ... group by event_id`, rather than one count per event. This measurement establishes bounded query counts for these fixtures; it is not a production latency benchmark.

## Billing persistence, exports and migration boundary

`definitionNameRetainsCaseAndDuplicateReturnsControlledConflict` explicitly suspends the test's outer transaction. Each HTTP mutation commits independently. JDBC verifies `MeNsAl  Jeep` after CREATE and `mensal  JEEP` after UPDATE, preserving the two internal spaces while removing only outer spaces. Detail, list, CSV and PDF retain the updated spelling; PDF text is extracted with PDFBox. A case variant receives controlled `CHARGE_DEFINITION_ALREADY_EXISTS`, while updating the same record succeeds. Only this test's synthetic definition is removed during cleanup.

No new migration/index or historical-name rewrite was introduced. MySQL validates the complete production Flyway chain through V14 and historical upgrades. PostgreSQL tests application behavior on a JPA-generated schema and executes V14 in isolation; these results do not establish PostgreSQL compatibility of the entire MySQL-oriented Flyway chain. Preexisting names that collide under V14's folded uniqueness can prevent migration of a legacy database; identifying/remediating production data is outside this PR and no production data was modified.

## Production files in the complete PR

- `publications/api/http/controller/member/EventController.java`
- `publications/api/http/dto/EventParticipantResponseDTO.java`
- `publications/api/http/dto/PublicationFeedDTO.java`
- `publications/core/application/service/EventService.java`
- `publications/core/application/service/PublicationFeedService.java`
- `publications/core/repository/EventParticipantRepository.java`
- `publications/infra/persistence/query/EventParticipantJpaQuery.java`

Paths are relative to `src/main/java/com/jeepclub/backend/`. No deployment, VPS operation, merge or Jira transition is part of this work.
