# Dependents Test Evidence

## Scope verified

The suite targets the current `ACTIVE`/`DISABLED` model backed by `dependents_dependent` and
`dependents_dependent_history`. Deletion archives a snapshot and hard-deletes the operational row.
Disabled dependents continue reserving CPF; history reserves `dependent_id`, not CPF.

## Evidence map

| Requirement | Evidence |
| --- | --- |
| Owner missing and owner inactive | `DependentServiceTest`, `DependentControllerTest` |
| CPF used by User or operational dependent | `DependentServiceTest`, `DependentControllerTest` |
| Concurrent operational CPF constraint translated | `DependentRepositoryAdapterTest` |
| Ownership for read/update/disable/enable/delete | `DependentServiceTest` |
| `ACTIVE -> DISABLED` and `DISABLED -> ACTIVE` | `DependentTest`, `DependentServiceTest` |
| Delete active and disabled dependents | `DependentServiceTest`, `DependentSystemFlowTest` |
| Pessimistic lock, archive snapshot and hard delete | `DependentRepositoryAdapterTest`, `DependentSystemFlowTest` |
| Delete rollback and unique historical `dependent_id` | `DependentRepositoryAdapterTest`, `DependentSystemFlowTest` |
| Invalid update phone and direct domain rejection | `DependentControllerTest`, `DependentTest` |
| Global `@PreAuthorize` denial response | `DependentSecurityIntegrationTest` |

## Execution log

| Command | Result | Evidence obtained on 2026-08-28 |
| --- | --- | --- |
| `./mvnw test` | Passed | 132 tests, 0 failures, 0 errors, 0 skipped |
| `./mvnw verify` | Passed | 132 tests, 0 failures, 0 errors, 0 skipped; package and JaCoCo reports generated |

Historical coverage percentages and PIT numbers were removed because they do not describe this version.
No new percentage or mutation score is claimed by this evidence file.

## Deliberate exclusions

- No endpoint was added for `enable()` or `disable()`.
- No migration, Flyway configuration or manual database change was introduced.
- No cross-table race between User CPF and Dependent CPF was redesigned.
- Medical profiles are not deleted or changed automatically. Their `ownerType`/`ownerId` retention semantics
  require a separate audit/LGPD decision because there is no physical foreign key to an operational dependent.
