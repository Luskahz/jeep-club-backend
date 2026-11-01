# Physical coverage and suite evidence

All comparisons below use physical JaCoCo counters summed by package. Format: percent (covered/missed). Baseline and final main tables merge separate Surefire and MySQL exec files against identical production bytecode within each measurement; percentages from isolated runs are never added. Production source is unchanged between measurements. Historical Jira is context, not an acceptance target.

| Module | Metric | Historical Jira | Current baseline | Final | Delta pp | Covered delta | Missed delta |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Identity | LINE | 91.25% | 93.3333% (952/68) | 94.3137% (962/58) | +0.9804 | +10 | -10 |
| Identity | BRANCH | 75.35% | 85.8120% (502/83) | 91.9658% (538/47) | +6.1538 | +36 | -36 |
| Authorization | LINE | 60.76% | 71.6049% (638/253) | 92.3681% (823/68) | +20.7632 | +185 | -185 |
| Authorization | BRANCH | 43.40% | 54.1985% (142/120) | 77.4809% (203/59) | +23.2824 | +61 | -61 |
| Publications | LINE | 91.62% | 93.7408% (1917/128) | 93.8386% (1919/126) | +0.0978 | +2 | -2 |
| Publications | BRANCH | 74.30% | 77.1446% (1259/373) | 82.7206% (1350/282) | +5.5760 | +91 | -91 |
| Platform | LINE | 90.89% | 92.5714% (972/78) | 94.4762% (992/58) | +1.9048 | +20 | -20 |
| Platform | BRANCH | 72.63% | 75.6957% (408/131) | 81.6327% (440/99) | +5.9370 | +32 | -32 |
| Shared | LINE | 99.71% | 100.0000% (412/0) | 100.0000% (412/0) | +0.0000 | +0 | +0 |
| Shared | BRANCH | 81.08% | 82.9268% (34/7) | 97.5610% (40/1) | +14.6342 | +6 | -6 |
| Platform + Shared | LINE | 93.39% | 94.6648% (1384/78) | 96.0328% (1404/58) | +1.3680 | +20 | -20 |
| Platform + Shared | BRANCH | 73.36% | 76.2069% (442/138) | 82.7586% (480/100) | +6.5517 | +38 | -38 |

## All five counters

| Module | Counter | Baseline covered/missed | Final covered/missed | Delta pp |
| --- | --- | --- | --- | --- |
| Identity | INSTRUCTION | 91.6630% (6223/566) | 92.6646% (6291/498) | +1.0016 |
| Identity | LINE | 93.3333% (952/68) | 94.3137% (962/58) | +0.9804 |
| Identity | BRANCH | 85.8120% (502/83) | 91.9658% (538/47) | +6.1538 |
| Identity | METHOD | 89.5270% (265/31) | 89.5270% (265/31) | +0.0000 |
| Identity | CLASS | 95.0617% (77/4) | 95.0617% (77/4) | +0.0000 |
| Authorization | INSTRUCTION | 72.6536% (3228/1215) | 89.9167% (3995/448) | +17.2631 |
| Authorization | LINE | 71.6049% (638/253) | 92.3681% (823/68) | +20.7632 |
| Authorization | BRANCH | 54.1985% (142/120) | 77.4809% (203/59) | +23.2824 |
| Authorization | METHOD | 69.2308% (198/88) | 86.7133% (248/38) | +17.4825 |
| Authorization | CLASS | 77.3196% (75/22) | 88.6598% (86/11) | +11.3402 |
| Publications | INSTRUCTION | 91.3505% (16951/1605) | 92.4122% (17148/1408) | +1.0617 |
| Publications | LINE | 93.7408% (1917/128) | 93.8386% (1919/126) | +0.0978 |
| Publications | BRANCH | 77.1446% (1259/373) | 82.7206% (1350/282) | +5.5760 |
| Publications | METHOD | 88.5430% (711/92) | 88.7920% (713/90) | +0.2490 |
| Publications | CLASS | 86.9565% (160/24) | 86.9565% (160/24) | +0.0000 |
| Platform | INSTRUCTION | 92.1602% (4902/417) | 94.4726% (5025/294) | +2.3124 |
| Platform | LINE | 92.5714% (972/78) | 94.4762% (992/58) | +1.9048 |
| Platform | BRANCH | 75.6957% (408/131) | 81.6327% (440/99) | +5.9370 |
| Platform | METHOD | 97.2222% (245/7) | 98.4127% (248/4) | +1.1905 |
| Platform | CLASS | 96.1039% (74/3) | 97.4026% (75/2) | +1.2987 |
| Shared | INSTRUCTION | 99.4882% (2916/15) | 99.7612% (2924/7) | +0.2730 |
| Shared | LINE | 100.0000% (412/0) | 100.0000% (412/0) | +0.0000 |
| Shared | BRANCH | 82.9268% (34/7) | 97.5610% (40/1) | +14.6342 |
| Shared | METHOD | 97.6744% (42/1) | 97.6744% (42/1) | +0.0000 |
| Shared | CLASS | 100.0000% (24/0) | 100.0000% (24/0) | +0.0000 |

## Surefire-only comparison

| Module | LINE baseline | LINE final | BRANCH baseline | BRANCH final |
| --- | --- | --- | --- | --- |
| Identity | 93.3333% (952/68) | 94.3137% (962/58) | 85.8120% (502/83) | 91.9658% (538/47) |
| Authorization | 71.6049% (638/253) | 92.3681% (823/68) | 53.8168% (141/121) | 77.4809% (203/59) |
| Publications | 93.6919% (1916/129) | 93.7897% (1918/127) | 76.7770% (1253/379) | 82.3529% (1344/288) |
| Platform | 92.5714% (972/78) | 94.4762% (992/58) | 75.6957% (408/131) | 81.6327% (440/99) |
| Shared | 100.0000% (412/0) | 100.0000% (412/0) | 82.9268% (34/7) | 97.5610% (40/1) |

## Test inventories

Categories inspect actual runner/context/HTTP/persistence mechanisms, including inherited fixtures. Primary categories below are mutually exclusive fixture-mechanism groups; test-mechanisms.csv retains overlapping tags. A mixed HTTP fixture may also contain direct service/query assertions (for example two of the nine new Authorization fixture cases). Concurrency tags identify fixtures containing coordinated scenarios, not every method in that class. Test names do not establish integration.

| Category | Baseline Surefire | Final Surefire | Delta |
| --- | --- | --- | --- |
| mysql-testcontainers | 0 | 0 | +0 |
| http-contract | 847 | 858 | +11 |
| persistence | 99 | 102 | +3 |
| spring-context | 195 | 211 | +16 |
| unit-with-mocks | 491 | 604 | +113 |
| pure-unit | 672 | 740 | +68 |

| Execution | Discovered | Passed | Failures | Errors | Skips |
| --- | --- | --- | --- | --- | --- |
| clean-test | 2515 | 2514 | 0 | 0 | 1 |
| verify | 2515 | 2514 | 0 | 0 | 1 |
| mysql | 479 | 479 | 0 | 0 | 0 |

New Surefire input cases: 211; removed: 0. New MySQL executions: 23; 21 repeat the newly added HTTP/Event scenarios and two are new MySQL contention cases. This yields 213 added scenario inputs across both runners, not 234 new business requirements. New JUnit method definitions: 122; existing bodies strengthened: 14 (see test-change-inventory.csv). New files: 12; modified files: 16 (six lifecycle-cleanup-only).

MySQL discovers 479 cases: 339 repeat scenarios actually discovered by Surefire and 140 have no direct inherited Surefire-case match (including MySQL-specific/migration cases; this does not assert 140 new business behaviors). clean-test and verify repeat the same Surefire suite. PIT repeats selected tests under mutation; its run count is never added to this inventory.

Windows has one conditional symlink skip. Exact-final-HEAD Linux CI must execute it; no @Disabled is introduced. LINE/BRANCH are execution measures; the traceability matrix and individual mutation decisions provide behavioral evidence.

Detailed package counters and absolute deltas: coverage-by-package.csv. Remaining partial methods/classes/branches: coverage-gaps-final.csv and final-merged.json. Declarative bootstraps, trivial accessors and redundant guards are not inflated into a 100% campaign. See the module reports for critical decisions and retained existing tests.

Raw XML/CSV/HTML: each preserved final-verify, final-mysql and final-merged directory contains site-jacoco. Exec files and logs are preserved separately. CI publishes maven-verify-evidence and mysql-migration-evidence; these runners are separate reports and are not falsely summed.
