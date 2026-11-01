# Publications — BACK-499 / BACK-500 / BACK-501 / BACK-502 / BACK-504

Surefire baseline: LINE 1916 covered / 129 missed (93.6919%); BRANCH 1253 / 379 (76.7770%). Historical Jira: 91.62% / 74.30%.

## Existing capabilities and reuse

Notice already has HTTP authentication/individual authorities, validation, partial-update shape, lifecycle and joined-persistence/history assertions. ServicePublication request and change-request tests already verify ownership, missing media, unchanged pending requests after failure, rejection without publication and real concurrent approval/rollback. Social feed already verifies like idempotency, database uniqueness, concurrent likes, comments/media, ownership and publication availability. These strong happy paths are reused.

The 75 existing EventOperationsIntegrationTest cases exercise real Event/Billing persistence, financial cutoff, ownership/capacity, guests, rides, cancellation, refunds, rollback and concurrent disputes. EventOperationsMySqlIT inherits those cases and adds MySQL-specific stale-snapshot/connection-pool checks. They are counted separately by execution profile, not added as unique business scenarios.

Boundaries to Vehicles, Dependents and Health in the Event service fixture are public module contracts. Dedicated lifecycle integrations separately prove the real provider composition. New cases use these same contracts; normal operations do not write other contexts' repositories.

## Residual decisions

EventService has line execution but missed branches in unpublished reads, no-seat/nonconfirmed ride eligibility, personal guest listing, invalid/duplicate ride responses and member reallocation around approved guests. These need behavior and final-state assertions, not another registration happy path.

EventReportService has substantial missed decisions despite high line coverage. The residual plan covers incompatible product/filter combinations before any boundary I/O, FULL/AVAILABLE transport capacity, absent finance/identity data, public-query batching at 500 and missing-identity emergency output. Existing privacy, historical ownership, optional charges and cutoff cases remain.

EventRegistration residual tests concern immutable allocation snapshots, dependent duplication across allocated/unallocated lists, duplicate vehicles, missing member allocation and terminal cancellation. Existing duplicated-member/dependent tests are reused.

Focal PIT selects EventRegistration, EventParticipationRequirement, EventService and EventReportService. It includes existing Event integration tests so mutations are also detected by actual orchestration/persistence. This does not imply the whole Publications surface was mutated.

## Acceptance

BACK-503 remains done by absorption into BACK-501. This stage adds 52 Surefire cases: 39 report cases, 10 inherited Event operations cases and three domain cases. Existing cases are also strengthened with exact report cells, per-household financial summaries, metadata and operational boundaries. No production source or migration is changed.

The first focused run passed 151 cases; the next successful expanded selection passed 157. A sparse-reference case then exposed a fixture missing mandatory registration IDs, corrected without weakening the domain constructor. The latest report/image/private-contract selection passes 95 cases (70 report, 21 image, four existing private-contract cases). Failed development attempts are retained as diagnostic evidence and excluded from acceptance totals.

The intermediate EventOperationsMySqlIT run passed 91 cases (81 baseline plus 10 inherited new scenarios). Final full MySQL execution must revalidate the last assertion refinements. Baseline focal PIT is 290 mutants: 161 KILLED, 108 SURVIVED, 21 NO_COVERAGE (55.5172%). The second focal run improved this to 266 KILLED, 23 SURVIVED, one NO_COVERAGE (91.7241%); remaining report-composition decisions are under final analysis. Final full-suite/merged physical counters and the final focal run supersede these intermediate results.

No full registration, Notice, service publication or social happy path is duplicated. The final matrix and mutation decisions distinguish real gaps from guards repeated by the same transactional pipeline.

## Stage mutation decisions and ownership refinement

The last focused report/private composition selection passed 74 cases (70 report and four private image contract cases). Stage Publications PIT before the final ownership refinement: **290 total, 279 KILLED, 10 SURVIVED, one NO_COVERAGE, zero TIMED_OUT/NON_VIABLE/execution errors; 96.2069%**, 1m55s. Production targets remain identical to baseline.

| Location | Remaining mutation | Individual decision |
| --- | --- | --- |
| EventReportService.batch:152 index 21 | Change loop boundary | Adds one empty public-query call at an exact batch boundary. Valid providers return no elements for an empty batch; row content and maximum-500 contract remain. Low extra-I/O edge; the 501-item test proves lossless bounded batches. |
| EventReportService.lambda$export$13:56 index 16 | Comparator returns zero | Member is assembled before its dependents; stable sorting retains that order when user and vehicle compare equal. The explicit member-first CSV assertion passes. Equivalent for assembled inputs. |
| EventReportService.lambda$financial$29:112 index 4 | Duplicate-state merge returns null (NO_COVERAGE) | Provider cannot produce duplicate user+definition for an event: EventChargeContext has unique(event_id,charge_definition_id) and cycle_id; MemberCharge has unique(user_id,cycle_id); EventBillingService emits one State per charge/context. Do not manufacture impossible duplicate financial rows merely to execute the merge. |
| EventService.allocate:45 index 53 | Remove lockReferences | locked already acquires references; validateDependents/validateAllocations repeat active/owner checks and public locks. All remain in the same transaction before save; actual invalid ownership/participant/capacity cases and MySQL disputes remain. No mock of redundant internal calls. |
| EventService.allocate:46 index 60 | Remove validateDependents | lockReferences already checks/locks every unallocated and allocated dependent through activeDependent. Same public rejection and persisted invariant. |
| EventService.allocate:47 index 67 | Remove validateAllocations | lockReferences already checks active vehicle owner and dependents; spare checks total capacity after save in the same rollback-capable transaction. Exact-full and overflow/rollback assertions pass. |
| EventService.lambda$allocate$1:42 index 6 | Omit dependent IDs from initial pre-lock collection | lockReferences and validations still acquire/validate every dependent before save. The mutation changes pre-acquisition/repetition, not the persistence invariant. Current production lock ordering is preserved; focal mutation testing does not establish every possible scheduling interleaving. |
| EventService.lambda$register$0:29 index 6 | Same pre-lock omission in registration | EventOperations.register explicitly calls lockReferences and validations before save. Same reasoning as allocation; existing real concurrent registration/lifecycle suites are retained. |
| EventService.lambda$allocate$2:48 index 14 | Include nonapproved guests in allocated-vehicle list | Domain constructor prohibits vehicle association unless APPROVED. Other statuses have null vehicle; matching previous positive vehicle IDs is unchanged. |
| EventService.respond:79 index 15 | Remove first requireOpen | rideRequests is invoked next and requires the same locked event to be open before any mutation. Closed-event tests still fail atomically. |

Remaining lock/pre-acquisition mutations are reviewed as repeated checks within the current transaction, not presented as proof that arbitrary lock removal is safe. Financial, capacity, guest lifecycle and public ownership decision mutants that change behavior are killed.

### Ownership survivor review correction

The earlier spare/guestSpace rationale for EventService.lambda$respond$8:82 index 7 was incorrect: spare validates that the vehicle belongs to its registered owner, but it does not bind that owner to the acting member. An unallocated/nonexistent vehicle alone could not distinguish this rule. The existing rideResponseRejectsUnknownGuestUnallocatedVehicleAndDuplicateAcceptance case now provisions another valid registered driver with available seats through public contracts, verifies that driver is eligible, rejects the first actor's offer for that other driver's vehicle, and asserts no offer/history/seat side effects. The production guard is correct and unchanged; the gap was regression detection. This ownership mutation must be killed, not accepted as redundant. Final focal results supersede the earlier survivor list.

Ownership refinement validation: the focused H2 case passes; EventOperationsMySqlIT passes **91 cases, zero failures/errors/skips** on the same assertions. Focal PIT now generates **290 mutants: 280 KILLED, nine SURVIVED, one NO_COVERAGE, zero timeout/nonviable/execution errors (96.5517%)**. The always-true lambda$respond$8 predicate is killed by rideResponseRejectsUnknownGuestUnallocatedVehicleAndDuplicateAcceptance. No new JUnit case was added for this correction; it strengthens one of the previously added workflow cases. Final clean full evidence remains a separate gate.
