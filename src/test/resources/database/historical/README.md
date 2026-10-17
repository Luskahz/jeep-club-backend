# Immutable historical Hibernate exports

These schema-only fixtures were exported independently by Hibernate 7.2.7.Final from
the audited JPA mappings, with MySQLDialect and the Spring naming strategies, before
future application mappings evolve. They contain no user records or Flyway history.

The Git blob provenance of these mappings is recorded in
`docs/database/mapping-audit.json`: V1 corresponds to e4aa00e, V2 adds the #93 profile,
and V3 adds #95 activation invitations. The operational comparison on MySQL 8.0.46
verified V1/V2/V3 independently; production required the separately documented
legacy collation reconciliation.

Do not regenerate these files from future entities. The MySQL integration test creates
legacy fixtures from these frozen SQL exports, explicitly baselines them and migrates
to the latest packaged SQL. Fresh databases migrate to that same latest version and
are compared with current JPA mappings. This avoids silently redefining history when
V4/V5 add or change an entity. SHA256SUMS records the initial exported files.
