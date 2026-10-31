# Platform/Shared — BACK-505 / BACK-506 / BACK-507 / BACK-509

Surefire baseline Platform: LINE 972 covered / 78 missed (92.5714%); BRANCH 408 / 131 (75.6957%). Shared: LINE 412 / 0 (100%); BRANCH 34 / 7 (82.9268%). Historical Jira Platform 90.89% / 72.63%; Shared 99.71% / 81.08%.

## Existing evidence retained

LocalFileStorageTest already uses real temporary files for path traversal, invalid namespaces/keys, collisions, missing objects/directories, interrupted writes, outside-root protection and symlinks. The Windows symlink case is conditionally skipped when link creation is unavailable; it must run on Linux CI. Production storage is not changed to make this test pass.

Private image namespaces, transaction compensation and rollback already have dedicated assertions. ImageMultipartAdapter and real multipart HTTP tests cover read failures without storage/persistence side effects. These recent BACK-538/539 cases are not duplicated.

Logging/context/filter tests already check enrichment, missing identity, MDC cleanup and transactional audit rollback. PermissionCatalogTest checks uniqueness and the catalog required by controllers. ExportRenderer and export HTTP tests already check formula escaping, limits, download headers and private-data minimization.

## Residual semantic decisions

- ImageMediaService: matching JPEG/WebP signatures and canonical MIME/extension; malformed format signatures, metadata/MIME mismatches and the exact 5-MB acceptance boundary without storage side effects.
- SystemLogServiceAdapter: a full real queue preserves existing events and drops new/requeued overflow; bounded drains; required persistence failures propagate.
- SystemLogPersistenceWorker: failed persistence requeues the real batch and the next explicit worker invocation persists it. No sleeps, scheduler timing or arbitrary test retries.
- ApiAccessDeniedHandler: real Problem writer produces sanitized RFC 9457 403 output, excluding synthetic exception/request secrets.
- ExportPages: exact and partial chunk boundaries, order/no duplicates and immediate propagation of query/consumer failures.
- StorageResource, ImageReference and StorageOperationException: absent/defensive binary content, namespace/traversal rejection and operation-specific sanitized messages with causes retained internally.

No getters-only, trivial records or declarative Spring configuration are added to the mutation scope. Shared's existing 100% LINE does not cover all these decisions.

The baseline MySQL run also exposed cached Spring contexts retaining Hikari pools after their Testcontainers had stopped. Six IT classes lack inherited after-class context closure (Registry already declares it explicitly). The residual infrastructure fix closes those contexts after each class; it preserves every assertion and migrated-schema scenario. The new Authorization MySQL fixture receives the same lifecycle. Expected logs from deliberately invalid migration/schema tests are distinguished from these unexpected stale-pool reconnection warnings.

Focal PIT selects ImageMediaService, SystemLogServiceAdapter, ApiAccessDeniedHandler, ExportPages, ImageReference, StorageResource and StorageOperationException. Existing consumer tests remain covered by the full regression suite.

## Acceptance

Final package counters, mutation outcomes, deterministic focused execution, full regressions and CI remain pending. BACK-508 remains done by absorption into BACK-507. No formal closure before review/integration.

## Stage validation

The initial focused selection passed 104 discovered cases: 103 executed/passed, one conditional Windows symlink skip. Forty cases are new; LocalFileStorage's 57 existing cases are reused. The intentional logging persistence failure is asserted and recovered by the next explicit worker invocation; its ERROR log is not a test error.

Focal PIT baseline: 58 mutants, 25 KILLED, four SURVIVED, 29 NO_COVERAGE (43.1034%). The first stage run produced 54 KILLED, two SURVIVED and two NO_COVERAGE (93.1034%), with zero timeouts/nonviable/execution errors. The existing PrivateImageMediaContractTest is now explicitly selected to exercise both private store/load mutations. Its four cases are reused, not new scenarios. The existing PNG upload now uses the exact eight-byte accepted signature boundary. Both changes retain the same seven production target classes/58 mutations.

Final full-suite counters, complete mutation decisions and exact-HEAD CI remain separate acceptance gates.

Final stage PIT: **58 total, 57 KILLED, one SURVIVED, zero NO_COVERAGE/TIMED_OUT/NON_VIABLE/execution errors; 98.2759%**, 17.878s. The existing private compensation case now composes the real ImageMediaService with a storage port, asserting namespace, returned key and rollback deletion; it no longer mocks the image service itself. This strengthens an existing scenario rather than adding a duplicate.

The remaining ImageMediaService.store:32 conditional-boundary mutant affects only an extension starting at filename index zero (for example .png). This is a LOW filename-metadata edge; canonical MIME/signature, size, public/private namespace and generated storage-key behavior are independently asserted. It is retained with an explicit decision rather than manufacturing a score-only case. Production behavior is unchanged.
