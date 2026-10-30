# Selection review supplement — actual Exposed SQL failures

This supplement adds two PostgreSQL regressions at **63e12a1a851498d62fa681b7c3bf2a655e3abf7d**. The production implementation and locally installed package1499c6b are unchanged. The [original source-bound report](STORAGE_SELECTION_91.md) and its5771-case archive remain immutable evidence for that source; the [supplement receipt](storage-selection-91-review.json) records the current5773-case inventory, actual artifact identity, original remote CI and additional raw reports.

## Dependency claim checked against execution

Copilot's [original finding](https://github.com/vypdev/vaultstadio/pull/117#discussion_r4388655926) says ExposedSQLException does not inherit SQLException. The installed/package-resolved **org.jetbrains.exposed:exposed-core:1.0.0**, SHA2564e6b44f76b5dc69bb611d64474f2a34ac444e4ec156a96ac8cdcccda3b2a0b7a, has the actual bytecode signature:

```java
public final class org.jetbrains.exposed.v1.exceptions.ExposedSQLException extends java.sql.SQLException
```

The existing SQL catch therefore already handles it and retains the wrapper/JDBC cause chain. No redundant wrapper catch, catch-all, cancellation suppression or Copilot change is introduced. This recurrence belongs to the existing [vypdev/copilot#736](https://github.com/vypdev/copilot/issues/736); the earlier consumer findings becoming obsolete did not establish an upstream source fix.

Two new actual migrated-PostgreSQL controls prove this boundary at the current storage adapter:

1. Duplicate a real storage primary key. The result is DatabaseException with the actual ExposedSQLException wrapper, underlying JDBC SQLState23505 and original row/bytes preserved after rollback.
2. Rename storage_items only in the isolated test container, execute the actual authenticated folder HTTP request through production composition, and assert500DATABASE_ERROR with safe context. Restore the table in finally; the same client then obtains200 and the original row/bytes remain. SQL table/driver diagnostics are not returned to the client.

These are executed database failures, rather than a mocked wrapper type. Both targeted controls pass and are subsequently collected by the unfiltered root backend selector. All42 current changed Kotlin hashes equal their63e12a1 Git blobs. The execution started atd619fa2 against the recorded working test source; committing that identical source during execution does not invent a different run.

## Updated inventory and remote distinction

Current local reports: **3249backend +1324desktop +1200Wasm =5773**, all failures/errors/skips0. Genuinely new backend cases rise from266 to268; client contracts remain8 per platform. The updated1518-case API selector ran completely; unchanged backend modules reused the earlier full asserted-source run, and unchanged frontend sources retain their separate complete JVM/Wasm runs. Existing gates and strict changed-source lint pass. Whole diagnostic backend coverage remains8958/16877lines and1934/6042branches, without exclusions or denominator changes.

The **original-head** [CI40503612712](https://github.com/vypdev/vaultstadio/actions/runs/40503612712) passes5771executions/1091XML atd619fa2. Four downloaded artifact digests are verified. Its actual checkoutb4ab0e7d2fa3c08fae71a3a0249de9d87f68aef7 has parentsc9f1ac5/d619fa2 and the same treed2c9cc4e982ffbbf475e12cf69330177b13625ab. These results are not represented as final-head CI for the later tests. Fresh CI, Copilot reconciliation and protected adoption remain required.

The actual packaged GUI400/recovery, preserved data and accepted03aa855 rollback remain as documented in the original report; no production source or package was changed by this supplement. #82/#84/#109/#111/#112/#115/#116, full platform acceptance and master remain separate work.
