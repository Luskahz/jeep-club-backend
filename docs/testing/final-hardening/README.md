# Final hardening — BACK-435, BACK-436, BACK-499, BACK-505

Baseline: `f52e152363151c5556dd5f04e9dc7435e02ffecd` (`origin/develop`, fetched 2026-10-31).
Branch: `test/back-435-436-499-505-final-hardening`. Target: `develop`.

Status: **GO COM RESSALVAS for review**. Technical implementation is complete at 56cce5b41fe5682a714fd96e39a8124bf9dd7475; [PR #125](https://github.com/Luskahz/jeep-club-backend/pull/125) targets develop. The delivery revision is the PR head containing these generated reports; its exact SHA and fresh delivery-head CI are recorded in the PR/Jira evidence. The source/test/pom/workflow tree after this measured commit is unchanged by report publication. Formal Jira closure awaits review and integration; no merge or deployment is performed.

## Scope ledger

| Stage | Story | Included subtasks | Historical exclusions |
| --- | --- | --- | --- |
| Identity | BACK-435 | BACK-491, BACK-492, BACK-493 | BACK-494 absorbed by BACK-493; remains done |
| Authorization | BACK-436 | BACK-495, BACK-496, BACK-497, BACK-498 | None |
| Publications | BACK-499 | BACK-500, BACK-501, BACK-502, BACK-504 | BACK-503 absorbed by BACK-501; remains done |
| Platform/Shared | BACK-505 | BACK-506, BACK-507, BACK-509 | BACK-508 absorbed by BACK-507; remains done |

All four stories and all 17 subtasks were read from Jira on 2026-10-31. Stories and residual subtasks were pending. Jira's earlier percentages refer to older code, including `0d8542fb3d90768573dc738cea85b23c1701b44c`; they are comparison context, not current evidence. BACK-505's old description mentions backlog planning; its Sprint field includes 239 and this execution is explicitly requested for it.

Authorized: residual tests, narrowly necessary bug fixes with regression, documentation, module commits, one PR, evidence comments and coherent Jira updates. Excluded: merge, master changes, deployment, VPS, production data, production migrations, changing coverage gates or functional rules to improve metrics.

## Recognition

The starting checkout was an unrelated Health branch; tracked files were clean. Existing untracked `output/` and `tmp/` are preserved and excluded from commits. The new branch was created directly from fetched `origin/develop`, without carrying unrelated branch commits.

Recent merged PRs inspected: [#117 Billing](https://github.com/Luskahz/jeep-club-backend/pull/117), [#118 Media](https://github.com/Luskahz/jeep-club-backend/pull/118), [#119 Event/lifecycle](https://github.com/Luskahz/jeep-club-backend/pull/119), [#120 onboarding notifications](https://github.com/Luskahz/jeep-club-backend/pull/120), [#121 Health/reference integrity](https://github.com/Luskahz/jeep-club-backend/pull/121). Searches found no existing open hardening PR or PR mentioning BACK-435/BACK-436. Their old test results are not reused as evidence for this branch.

## Execution environment and mechanisms

- Windows 10 amd64; process-local Microsoft JDK 17.0.18; wrapper Maven 3.9.14.
- Resolved JUnit Jupiter API/engine and Platform 6.0.3 (existing Jupiter APIs, not a dependency upgrade); JaCoCo 0.8.15; PIT 1.19.0 with pitest-junit5-plugin 1.2.3; Surefire/Failsafe 3.5.5.
- Testcontainers 2.0.4; MySQL image 8.0.46; MySQL Connector/J 9.6.0; Spring Boot 4.0.4; Docker 28.5.1. Versions were read from the resolved runtime classpath.
- Default Surefire naming discovers `*Test` and `*Tests`, including Spring/persistence/HTTP tests. Names alone do not determine test type.
- `mysql-migrations` activates Failsafe integration-test/verify; `*IT` subclasses inherit real scenarios and use MySQL 8.0.46/Testcontainers. `skipUnitTests=true` skips Surefire only.
- GitHub Actions has separate Maven verify and MySQL migrations gates. MySQL also runs a future-migration probe in a disposable copy. Deployment is conditional on push to master and is excluded here.
- Existing PIT defaults target Dependents. The four `*-hardening-mutation` profiles override both targetClasses and targetTests and clear inherited exclusions. They do not run the default mutation scope.

## Reproduction

PowerShell: set `$env:JAVA_HOME='C:\Users\lucas.l\.jdks\ms-17.0.18'`; use `mvnw.cmd` in place of `./mvnw` below. Run heavy suites sequentially. Keep every dotted -D argument quoted in PowerShell; otherwise it may be split before Maven receives it. The run.ps1 entrypoint below passes an argument array safely. Preserve logs, XML and exec files outside `target` before another clean/build.

```sh
./mvnw --batch-mode --no-transfer-progress clean test
./mvnw --batch-mode --no-transfer-progress '-Djacoco.destFile=target/hardening-unit.exec' '-Djacoco.dataFile=target/hardening-unit.exec' verify
./mvnw --batch-mode --no-transfer-progress -Pmysql-migrations '-DskipUnitTests=true' '-Djacoco.destFile=target/hardening-mysql.exec' '-Djacoco.dataFile=target/hardening-mysql.exec' verify
./mvnw -Phardening-coverage-merge jacoco:merge jacoco:report
python docs/testing/final-hardening/measure.py --coverage target/site/jacoco/jacoco.xml --reports target/surefire-reports --out measured.json
./mvnw -Pidentity-hardening-mutation org.pitest:pitest-maven:mutationCoverage
./mvnw -Pauthorization-hardening-mutation org.pitest:pitest-maven:mutationCoverage
./mvnw -Ppublications-hardening-mutation org.pitest:pitest-maven:mutationCoverage
./mvnw -Pplatform-shared-hardening-mutation org.pitest:pitest-maven:mutationCoverage
```

The complete Windows sequence is automated by the checked-in runner (choose a new evidence folder for every attempt):

```powershell
.\docs\testing\final-hardening\run.ps1 -Phase final -EvidenceDirectory 'C:\outside-repository\new-hardening-attempt'
```

PIT's direct mutationCoverage goal does not compile tests. After editing Java/tests, first run the affected tests or mvnw test-compile; never measure stale target/test-classes. The full runner always starts with a clean compilation.

Only merge exec data from the same compiled production classes and deliberately selected runs. A report from a failed/incomplete suite is diagnostic and is not a passing gate. Do not add percentages or average class percentages. `measure.py` sums physical package counters for all five metrics, inventories unique XML testcase IDs and records runner/context mechanisms (including inheritance). Categories are mutually exclusive; mechanism tags retain overlap. Repeated Maven executions are reported separately, never added as new test cases.

PIT reports are under `target/pit-reports/{identity,authorization,publications,platform-shared}`; JaCoCo XML/CSV/HTML are under `target/site/jacoco`. Local raw artifacts are preserved outside the repository in `C:\Users\lucas.l\.codex\tmp\jeep-final-hardening-f52e1523`. Raw logs can contain generated test secrets and are not versioned.

## Final results and evidence

- [Structured final assessment A–H](final-assessment.md).
- [Physical coverage, historical/current/final and all five counters](coverage-summary.md); [package counters](coverage-by-package.csv); [remaining partial methods/branches](coverage-gaps-final.csv).
- [Focal mutation summary and exact selected classes](mutation-summary.md); [all generated mutations](mutation-inventory.csv); [individual unresolved decisions](mutation-decisions.csv); [each initial gap's final disposition/killing test](mutation-baseline-resolution.csv).
- [Traceability matrix](traceability-matrix.csv); [new case IDs](new-test-cases.csv); [new/strengthened methods](test-change-inventory.csv); [actual fixture mechanisms](test-mechanisms.csv).
- [Execution manifest, raw XML hashes and unexecuted declarations](execution-evidence.json); [implementation CI evidence/artifacts](ci-code-evidence.json); [findings, intermediate failures and resolutions](findings.md).

The local clean test and independent verify each discover 2515 cases: 2514 passed, zero failures/errors and one conditional Windows symlink skip. Linux CI executes all 2515 with zero skips. Full MySQL discovers 479 cases, zero failures/errors/skips. CI's forward-migration archive repeats the same 479 cases; it is not another set of business scenarios.

Added 211 Surefire case inputs and two MySQL-only contention inputs (213 unique added JUnit inputs); 122 new method definitions and 14 preexisting bodies strengthened, in 12 new and 16 modified files (six lifecycle cleanup only). Multiple assertions/variants within an existing case do not inflate this inventory. The matrix has 64 behavior groups: 34 already covered, 24 partial, four absent and two not applicable at baseline; final concrete evidence covers the 62 applicable groups. No test is added merely for a getter/record/configuration or a 100% target.

The accepted nonblocking limitations are explicit: focal mutation scope rather than whole-project mutation; justified repeated/unreachable/LOW edges; two dependent pre-lock collection mutants retain validation/locks before persistence but mutation testing does not prove every possible scheduling interleaving. Current production lock ordering is preserved. The acting-member vehicle ownership mutant is killed and is not accepted as redundant. The Windows symlink condition is covered by Linux CI.

Measured implementation SHA is immutable above. A document cannot embed its own eventual commit hash; the final report-publication SHA is therefore recorded externally in the PR/Jira and final delivery response. Fresh delivery-head CI verifies that exact revision. Raw local artifacts are under final-run3; final-verify/final-mysql/final-merged contain JaCoCo XML/CSV/HTML and exec files, and final-pit-{scope}/pit contains PIT XML/HTML. They are preserved outside target before every subsequent build. Reproduction commands above retain the same separation.
