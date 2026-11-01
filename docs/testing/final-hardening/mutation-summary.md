# Focal mutation evidence

PIT 1.19.0/JUnit 5 plugin 1.2.3; Java 17; two workers; inherited default mutators. Production targets and total generated mutations remain constant between baseline and final. Target test filters are explicit: Authorization adds its new real HTTP fixture; Platform also selects the preexisting private image contract fixture. These are selected classes, not module-wide/project-wide scores.

| Scope | Total | Baseline K/S/NC | Final K/S/NC | Baseline score | Final score | Delta pp | Final wall time |
| --- | --- | --- | --- | --- | --- | --- | --- |
| identity | 137 | 92/16/29 | 135/2/0 | 67.1533% | 98.5401% | +31.3868 | 57.994 s |
| authorization | 160 | 71/28/61 | 157/3/0 | 44.3750% | 98.1250% | +53.7500 | 02:54 min |
| publications | 290 | 161/108/21 | 280/9/1 | 55.5172% | 96.5517% | +41.0345 | 01:54 min |
| platform-shared | 58 | 25/4/29 | 57/1/0 | 43.1034% | 98.2759% | +55.1725 | 17.528 s |

K/S/NC = KILLED/SURVIVED/NO_COVERAGE. Every final run has zero TIMED_OUT, NON_VIABLE, RUN_ERROR, MEMORY_ERROR and NOT_STARTED. No timeout is treated as an acceptable kill. See final-mutation.json for explicit zeros and exact classes.

296 initially unresolved mutations are traced in mutation-baseline-resolution.csv. 16 remain with individual decisions in mutation-decisions.csv; every other baseline gap now names the killing test. No previously killed mutant regresses. mutation-inventory.csv retains all generated mutations with final status and killing test.

## Selected production classes

### identity

- com.jeepclub.backend.iam.identity.core.application.service.user.UserQueryService
- com.jeepclub.backend.iam.identity.core.domain.model.User

### authorization

- com.jeepclub.backend.iam.authorization.core.application.service.role.AdminRoleService
- com.jeepclub.backend.iam.authorization.core.application.service.rolepermission.AdminRolePermissionService
- com.jeepclub.backend.iam.authorization.core.application.service.userrole.AdminUserRoleService
- com.jeepclub.backend.iam.authorization.core.domain.model.Permission
- com.jeepclub.backend.iam.authorization.core.domain.model.Role

### publications

- com.jeepclub.backend.publications.core.application.service.EventReportService
- com.jeepclub.backend.publications.core.application.service.EventService
- com.jeepclub.backend.publications.core.application.service.internal.EventParticipationRequirement
- com.jeepclub.backend.publications.core.domain.model.EventRegistration

### platform-shared

- com.jeepclub.backend.platform.logging.SystemLogServiceAdapter
- com.jeepclub.backend.platform.security.handler.ApiAccessDeniedHandler
- com.jeepclub.backend.platform.storage.image.ImageMediaService
- com.jeepclub.backend.shared.export.ExportPages
- com.jeepclub.backend.shared.storage.ImageReference
- com.jeepclub.backend.shared.storage.StorageResource
- com.jeepclub.backend.shared.storage.exception.StorageOperationException

## Interpretation

Remaining equivalents/repeated guards are not suppressed through exclusions. The financial duplicate-state merger is unreachable under actual provider uniqueness constraints; no invalid cross-module fixture is invented to cover it. Two pre-lock collection mutants retain subsequent public locks before save: their scheduling limitation is documented, and actual current production ordering remains unchanged. The narrow leading-dot filename edge is LOW. Permissions, financial eligibility, capacity and effective lifecycle changes are not accepted merely because line coverage is high.

## Reproduction and artifacts

Use the four named profiles documented in README; never the default Dependents PIT scope. Reports are preserved in final-run3/final-pit-{scope}/pit/mutations.xml and index.html outside target, plus individual Maven logs and parsed JSON. Baseline raw reports remain in baseline-pit-{scope}/pit. No .exec data or percentage from a previous SHA is reused for the final measurements.

For baseline reproduction, infrastructure commit 2802e46f has exactly the same src production/test tree as f52e1523 and adds only the explicit profiles/tooling/CI evidence preservation. Final mutation filters additionally select the new Authorization HTTP class and existing private image class as disclosed above. PIT repeats tests under mutation; its execution count is never added to unique scenario totals.
