# Final hardening — BACK-435, BACK-436, BACK-499, BACK-505

Baseline: `f52e152363151c5556dd5f04e9dc7435e02ffecd` (`origin/develop`, fetched 2026-10-31).
Branch: `test/back-435-436-499-505-final-hardening`. Target: `develop`.

Status: **audit in progress; no completion or GO claimed**. Final SHA, PR, CI and results will be recorded after execution.

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
- JaCoCo 0.8.15; PIT 1.19.0, inherited JUnit 5 plugin; Surefire 3.5.5.
- Default Surefire naming discovers `*Test` and `*Tests`, including Spring/persistence/HTTP tests. Names alone do not determine test type.
- `mysql-migrations` activates Failsafe integration-test/verify; `*IT` subclasses inherit real scenarios and use MySQL 8.0.46/Testcontainers. `skipUnitTests=true` skips Surefire only.
- GitHub Actions has separate Maven verify and MySQL migrations gates. MySQL also runs a future-migration probe in a disposable copy. Deployment is conditional on push to master and is excluded here.
- Existing PIT defaults target Dependents. The four `*-hardening-mutation` profiles override both targetClasses and targetTests and clear inherited exclusions. They do not run the default mutation scope.

## Reproduction

PowerShell: set `$env:JAVA_HOME='C:\Users\lucas.l\.jdks\ms-17.0.18'`; use `mvnw.cmd` in place of `./mvnw` below. Run heavy suites sequentially. Preserve logs, XML and exec files outside `target` before another clean/build.

```sh
./mvnw --batch-mode --no-transfer-progress clean test
./mvnw --batch-mode --no-transfer-progress -Djacoco.destFile=target/hardening-unit.exec -Djacoco.dataFile=target/hardening-unit.exec verify
./mvnw --batch-mode --no-transfer-progress -Pmysql-migrations -DskipUnitTests=true -Djacoco.destFile=target/hardening-mysql.exec -Djacoco.dataFile=target/hardening-mysql.exec verify
./mvnw -Phardening-coverage-merge jacoco:merge jacoco:report
python docs/testing/final-hardening/measure.py --coverage target/site/jacoco/jacoco.xml --reports target/surefire-reports --out measured.json
./mvnw -Pidentity-hardening-mutation org.pitest:pitest-maven:mutationCoverage
./mvnw -Pauthorization-hardening-mutation org.pitest:pitest-maven:mutationCoverage
./mvnw -Ppublications-hardening-mutation org.pitest:pitest-maven:mutationCoverage
./mvnw -Pplatform-shared-hardening-mutation org.pitest:pitest-maven:mutationCoverage
```

Only merge exec data from the same compiled production classes and deliberately selected runs. A report from a failed/incomplete suite is diagnostic and is not a passing gate. Do not add percentages or average class percentages. `measure.py` sums physical package counters for all five metrics, inventories unique XML testcase IDs and records runner/context mechanisms (including inheritance). Categories are mutually exclusive; mechanism tags retain overlap. Repeated Maven executions are reported separately, never added as new test cases.

PIT reports are under `target/pit-reports/{identity,authorization,publications,platform-shared}`; JaCoCo XML/CSV/HTML are under `target/site/jacoco`. Local raw artifacts are preserved outside the repository in `C:\Users\lucas.l\.codex\tmp\jeep-final-hardening-f52e1523`. Raw logs can contain generated test secrets and are not versioned.
