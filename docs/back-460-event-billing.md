# BACK-460 — Event cancellation and financial integrity

Base: `origin/develop@7c8df2e39dd218d5cbc6eb6042d3151b511840b9`.
Branch: `feat/back-460-event-billing-fixes`; PR destination: `develop`.
This is the first of six stacked PRs. BACK-510 will use this branch as its temporary base.
No integration or deployment is authorized by this delivery.

## Acceptance and decisions

| Subtask | Implemented behavior | Evidence |
| --- | --- | --- |
| BACK-464 | Cancel every context of the selected Event, including FINISHED and ARCHIVED. Open charges become CANCELED; PAID charges and all payments remain. Generate a single eligibility for confirmed payments. | EventOperationsIntegrationTest, EventOperationsMySqlIT, BillingEventBoundaryTest |
| BACK-465 | A payment confirmed after cancellation gets a full 30-day window starting at `max(canceledAt, confirmedAt)`. Pending/rejected receipts do not produce refunds. | Day 29/30/31 and exact expiration tests; duplicate confirmation/request tests |
| BACK-466 | Individual withdrawal preserves financial obligations; terminate pending personal guest requests and accepted ride offers, preserve registration/audit and serialize repeated cancellation. Approved guests prevent withdrawal until administratively resolved. | Individual withdrawal, guest conflict, payment/refund and concurrency integration tests |

Individual withdrawal has no automatic debt waiver or refund. This is an explicit conservative interpretation of the unspecified business policy, requiring product review: the alternative is a waiver and cancellation-specific refund rule. Existing Billing member-request and administrative review remain available for confirmed payments. Pending payments remain reviewable and can settle the retained debt. No Publications service writes Billing repositories.

For Event cancellation, the generic Billing cycle-cancel endpoint retains its original GENERATED-only rule. The Event-specific application operation accepts a previously finished cycle, retaining `finishedAt/finishedByUserId`; an archived cycle remains ARCHIVED and gains cancellation metadata. An archived cycle's `canceledAt` is authoritative for later payment confirmation. Neither closing audit nor money is erased. Repeated Event cancellation returns the existing Event without resetting its financial dates or windows.

Refund eligibility is half-open: `[eligibleAt, eligibleUntil)`. Requests and direct approval of an unrequested eligibility fail exactly at the deadline. An already timely REQUESTED refund can be reviewed later, following the existing domain. A repeated payment-confirm command still returns a state conflict and cannot create a second refund. A repeated successful cancellation returns success.

## Architecture and concurrency

Publications owns Event/Registration/guest/offer decisions; `EventBillingCommand` remains the cross-module boundary. Billing visits only `EventChargeContext` rows of the requested Event, in definition ID order; ALL_MEMBERS/USER/ROLE and other Event cycles remain untouched. No public contract signature or permission changes.

Cancellation and confirmation coordinate on the cycle row, then payment rows, then charge rows. Finish/archive also lock the cycle. Confirmation obtains the immutable cycle ID through a scalar query before locking, avoiding a stale mutable entity in the JPA persistence context. Locked aggregate adapters detach the previously managed instance and reload under the same database lock: an ordinary Hibernate refresh was proven insufficient by the MySQL snapshot regression. Event context and member-charge lookup also use current locking reads after the parent lock, so a pre-lock consistent snapshot cannot hide a concurrently committed context/charge. Eligibility checks and payment enumeration use locking current reads so MySQL REPEATABLE READ cannot hide a refund or confirmed payment committed while cancellation waited. The payment lock remains the common serialization point with direct member refund requests.

The Event root serializes individual withdrawal against guest review, ride selection and repeated cancellation. An accepted offer becomes DECLINED; its original response timestamp is retained. Administrative guest requests remain available to other registrations. A selected offer/approved guest blocks withdrawal without partial changes. Personal pending guest requests become REJECTED with reason `REGISTRATION_CANCELLED`, actor and review timestamp.

## Persistence and rollout

No schema change, migration, environment variable, volume or storage change. V1–V4 remain intact and V5 remains available for the next necessary schema change. All financial/operational rows remain; this delivery has no file deletion or import. New code accepts the combination of finishing and subsequent Event cancellation metadata, so downgrading to older code after such a cancellation can fail reconstruction. Use a forward correction or rehearse a data-aware restore; do not clear audit fields to force rollback.

Before a future release: review all accumulated PRs including #103/#104; verify exact deployed SHA and Flyway state, backups and isolated restore, MySQL tests and Hibernate validate, financial reconciliation for canceled/finished/archived Events, member/admin permissions and frontend behavior. Keep both Membership Registry operational flags false until separate approval. SMTP/profile and bootstrap issues recorded in #103/#104 require fresh operational evidence by the infrastructure agent. Nothing here authorizes VPS access.

## HTTP and frontend

Existing method/path/DTO/security contracts are preserved; OpenAPI now describes the cancellation effects. `POST /admin/events/{id}/cancel` returns 200 idempotently. `POST /events/{id}/registrations/me/cancel` preserves financial obligations and returns the canceled registration on repetition. The UI must not label a withdrawn registration as debt-forgiven or automatically refunded; show Billing status separately. An approved guest produces the existing controlled 409 and needs administrative resolution before retry.

## Validation

Final code commit: `16265a1cbd90cf5b5f3746f22396c5789f7347cc`. Later commits contain documentation/evidence only.

| Gate | Cases | Failures | Errors | Skips |
| --- | ---: | ---: | ---: | ---: |
| Full `mvnw.cmd --batch-mode --no-transfer-progress verify` | 1,951 | 0 | 0 | 1 |
| Complete `-Pmysql-migrations -DskipUnitTests=true verify` | 99 | 0 | 0 | 0 |
| `bash deploy/test-future-migration.sh` with temporary V5 | 99 | 0 | 0 | 0 |
| Focused `-Dit.test=EventOperationsMySqlIT` | 49 | 0 | 0 | 0 |

These are gate execution counts, not a count of unique cases across repeated runs. The single skip is the pre-existing Windows symbolic-link capability test. MySQL suites comprise migration/schema validation (14), Registry persistence/security/concurrency (28), Registry onboarding (8) and Event operations/concurrency (49). The forward probe executed the unchanged tracked script using a Windows environment adapter for Python3 and verified workspace-confined temporary cleanup. Its V5 never entered the release SQL directory. CI is observed after PR publication and recorded in the PR description; these local results do not predict CI.

Machine-readable [sanitized evidence](back-460-test-evidence.json). Both cancellation requests keep their existing body-free contract; the authenticated principal supplies the actor. Performance under production-scale Event volume was not load tested; fresh locking reloads deliberately add queries to preserve correctness.

Final-tree Maven, real MySQL, forward-migration and CI results are recorded here after execution. The shared MySQL integration class repeats the complete Event suite against MySQL 8.0.46 with Flyway and Hibernate validate, including synthetic data only. H2 success alone is insufficient. The first full MySQL run found four race/snapshot failures after H2 passed; those regressions remain enabled and drive the current-read correction, rather than being skipped or weakened.

Known review points: individual debt retention and approved-guest restriction are deliberate business choices, not silently waived obligations. This PR preserves the approved-guest guard and does not add an endpoint to undo approval; the administrative resolution workflow remains outside this delivery and must be considered during review. Ride-offer withdrawal uses the existing DECLINED vocabulary and preserves its original response time; a dedicated withdrawal audit vocabulary would be a separate contract/schema evolution. Full retention policy remains BACK-487.
