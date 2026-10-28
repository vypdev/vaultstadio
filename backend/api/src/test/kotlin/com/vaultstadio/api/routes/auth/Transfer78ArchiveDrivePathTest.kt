package com.vaultstadio.api.routes.auth

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Creation uses real production HTTP; unsupported portable archive names must fail before any ZIP. */
internal class Transfer78ArchiveDrivePathTest : Transfer78HttpBase() {
    @ParameterizedTest
    @ValueSource(strings = ["root-folder", "nested-folder", "root-file", "nested-file"])
    fun `TRANS78-ARCH07 drive-prefixed root and descendant names cannot yield attachment bytes`(case: String) = app {
        val owner = client.owner78()
        val parent = if (case.startsWith("nested")) client.folder78(owner, "safe-root") else null
        val item = if (case.endsWith("folder")) {
            client.folder78(owner, "C:", parent)
        } else {
            client.upload78(owner, name = "C:payload.bin", parent = parent)
        }
        val before = fixture.contentHashes()
        val response = client.zip78(owner, listOf(parent ?: item))
        assertSafeFailure(response, HttpStatusCode.BadRequest)
        assertNull(response.headers[HttpHeaders.ContentDisposition])
        assertEquals(before, fixture.contentHashes())
    }
}
