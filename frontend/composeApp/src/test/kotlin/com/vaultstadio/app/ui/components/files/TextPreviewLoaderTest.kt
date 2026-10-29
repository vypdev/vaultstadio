package com.vaultstadio.app.ui.components.files

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.usecase.DownloadFileUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TextPreviewLoaderTest {
    @Test fun api116TextUsesTheDomainPortWithTheExactItemId() = runTest {
        val id = "é /?"
        val port = port { actual ->
            assertEquals(id, actual)
            Result.Success("content ☃".encodeToByteArray())
        }
        assertEquals("content ☃", assertIs<Result.Success<String>>(loadTextPreview(port, id)).data)
    }

    @Test fun api116TextRetainsTheDisplayCap() = runTest {
        val text = assertIs<Result.Success<String>>(
            loadTextPreview(
                port {
                    Result.Success(ByteArray(102401) { 65 })
                },
                "id",
            ),
        ).data
        assertEquals(102400, text.takeWhile { it == 'A' }.length)
        assertTrue(text.endsWith("(truncated - file too large for preview)"))
    }

    @Test fun api116EmptyFileRemainsAnEmptySuccess() = runTest {
        assertEquals(
            "",
            assertIs<Result.Success<String>>(
                loadTextPreview(
                    port {
                        Result.Success(byteArrayOf())
                    },
                    "id",
                ),
            ).data,
        )
    }

    @Test fun api116PortFailureIsNotEmptyContent() = runTest {
        assertIs<Result.Error>(loadTextPreview(port { Result.Error("DENIED", "untrusted response") }, "id"))
    }

    @Test fun api116ThrownFailureHasSafeCopy() = runTest {
        val error =
            assertIs<Result.Error>(loadTextPreview(port { throw IllegalStateException("private://sensitive") }, "id"))
        assertEquals("Failed to load content. Try again later.", error.message)
    }

    @Test fun api116CancellationKeepsItsMeaning() = runTest {
        assertFailsWith<CancellationException> { loadTextPreview(port { throw CancellationException("cancel") }, "id") }
    }
    private fun port(read: suspend (String) -> Result<ByteArray>) = object : DownloadFileUseCase {
        override suspend fun invoke(itemId: String) = read(itemId)
        override fun getDownloadUrl(
            itemId: String,
        ): String = error("preview must use the authenticated download operation")
    }
}
