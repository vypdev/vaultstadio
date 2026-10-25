# Exposed 1.0 compatibility

The independent backend uses Exposed 1.0 with the Kotlin foundation from #24.
Framework imports follow the upstream `org.jetbrains.exposed.v1.core` and
`org.jetbrains.exposed.v1.jdbc` namespaces. Query predicates are top-level
extensions; pagination preserves both limit and offset through the separate
query methods.

The existing domain model uses `kotlinx.datetime.Instant`. Table declarations
use upstream `xTimestamp` (imported as `timestamp`) to preserve that model and
the existing SQL timestamp column declarations. This is the compatibility
adapter supplied by Exposed, not a domain-wide migration to `kotlin.time`.
No automatic schema migration is added by this maintenance update.

Validation includes the existing PostgreSQL/storage infrastructure suite:
318 tests, zero failures/errors/skips, plus backend compilation, ktlint and
Detekt with the current rule configuration. API and full cross-platform CI
remain required before merge. The update was ported to the actual backend
catalog; the removed root Gradle build stays removed.

References: [upstream migration guide](https://www.jetbrains.com/help/exposed/migration-guide-1-0-0.html)
and [versioned datetime adapter sources](https://repo.maven.apache.org/maven2/org/jetbrains/exposed/exposed-kotlin-datetime/1.0.0/exposed-kotlin-datetime-1.0.0-sources.jar).
