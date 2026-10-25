# Exposed 1.0 compatibility

The standalone backend uses Exposed 1.0 as part of the consolidated dependency
update in issue #62. Imports follow the upstream `org.jetbrains.exposed.v1.core`
and `org.jetbrains.exposed.v1.jdbc` namespaces. Query predicates are top-level
extensions; separate `limit` and `offset` calls preserve existing pagination.

The domain model still uses `kotlinx.datetime.Instant`. Table declarations use
upstream `xTimestamp` (imported as `timestamp`) with the backend datetime
compatibility artifact to preserve the existing model and SQL column declarations.
No schema migration or domain-wide migration to `kotlin.time` is introduced.

The existing PostgreSQL repository, storage and API suites verify the migrated
persistence layer. See [the consolidated update inventory](DEPENDENCY_UPDATE_62.md)
for the related dependency changes and validation evidence.

References: [upstream migration guide](https://www.jetbrains.com/help/exposed/migration-guide-1-0-0.html)
and [versioned datetime adapter sources](https://repo.maven.apache.org/maven2/org/jetbrains/exposed/exposed-kotlin-datetime/1.0.0/exposed-kotlin-datetime-1.0.0-sources.jar).
