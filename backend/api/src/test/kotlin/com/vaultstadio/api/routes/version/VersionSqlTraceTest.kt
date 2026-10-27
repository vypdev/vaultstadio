package com.vaultstadio.api.routes.version

import com.vaultstadio.infrastructure.persistence.ExposedVersionItemScopeRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionSqlTraceTest {
    @Test
    fun `C89-TRACE SQL boundaries exclude fixture reads without hiding real protected queries`() = runBlocking {
        val f = VersionIntegrationFixture()
        f.start()
        try {
            f.reset()
            val trace = VersionSqlTrace(f.storage.postgres)
            val (_, fullRead) = trace.capture { f.snapshot() }
            assertTrue(fullRead.contains("SELECT file_versions.id, file_versions.item_id"), fullRead)
            val before = f.snapshot()
            val (scope, minimalRead) = trace.capture { ExposedVersionItemScopeRepository().findItemId(f.old.id) }
            val after = f.snapshot()
            assertEquals(f.item.id, scope.fold({ throw it }, { it }))
            assertEquals(before, after)
            val versionQueries = minimalRead.lines().filter { it.contains("SELECT") && it.contains("file_versions") }
            assertEquals(1, versionQueries.size, minimalRead)
            assertTrue(
                versionQueries.single().contains("SELECT file_versions.item_id FROM file_versions"),
                minimalRead,
            )
        } finally {
            f.close()
        }
    }
}
