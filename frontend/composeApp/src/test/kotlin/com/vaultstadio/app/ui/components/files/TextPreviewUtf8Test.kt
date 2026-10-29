package com.vaultstadio.app.ui.components.files

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.usecase.DownloadFileUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TextPreviewUtf8Test {
    @Test fun api116TwoByteSplit() = runTest { split("é", 1) }

    @Test fun api116ThreeByteFirstSplit() = runTest { split("€", 1) }

    @Test fun api116ThreeByteSecondSplit() = runTest { split("€", 2) }

    @Test fun api116FourByteFirstSplit() = runTest { split("🙂", 1) }

    @Test fun api116FourByteSecondSplit() = runTest { split("🙂", 2) }

    @Test fun api116FourByteThirdSplit() = runTest { split("🙂", 3) }

    @Test fun api116CompleteTwoByteBoundary() = runTest { complete("é") }

    @Test fun api116CompleteThreeByteBoundary() = runTest { complete("€") }

    @Test fun api116CompleteFourByteBoundary() = runTest { complete("🙂") }

    @Test fun api116GenuineReplacementCharacterIsContent() = runTest { complete("\uFFFD") }

    @Test fun api116OverlongTwoByteSourceRetainsReplacement() = runTest { malformed(0xC0, 0x80, 65) }

    @Test fun api116OverlongThreeByteSourceRetainsReplacement() = runTest { malformed(0xE0, 0x80, 0x80, 65) }

    @Test fun api116SurrogateSourceRetainsReplacement() = runTest { malformed(0xED, 0xA0, 0x80, 65) }

    @Test fun api116OutOfRangeSourceRetainsReplacement() = runTest { malformed(0xF4, 0x90, 0x80, 0x80, 65) }

    @Test fun api116IncompleteSourceTailRetainsReplacement() = runTest { malformed(0xF0, 0x9F) }

    @Test fun api116MissingContinuationRetainsReplacement() = runTest { malformed(0xE2, 65, 65) }

    @Test fun api116StrayContinuationCannotRemoveCompleteAscii() = runTest {
        val prefix = "A".repeat(LIMIT)
        preview(prefix.encodeToByteArray() + byteArrayOf(0x80.toByte(), 65), prefix + NOTICE)
    }

    @Test fun api116LongMalformedContinuationRunRetainsReplacements() = runTest {
        val prefix = "A".repeat(LIMIT - 4)
        preview(
            prefix.encodeToByteArray() + ByteArray(5) { 0x80.toByte() } + byteArrayOf(65),
            prefix + "\uFFFD".repeat(4) + NOTICE,
        )
    }

    @Test fun api116EarlierMalformedByteIsNotHiddenByValidBoundaryRepair() = runTest {
        val prefix = "A".repeat(LIMIT - 2)
        preview(
            prefix.encodeToByteArray() + byteArrayOf(0xFF.toByte()) + "€tail".encodeToByteArray(),
            prefix + "\uFFFD" + NOTICE,
        )
    }

    private suspend fun split(character: String, includedBytes: Int) {
        val prefix = "A".repeat(LIMIT - includedBytes)
        preview((prefix + character + "tail").encodeToByteArray(), prefix + NOTICE)
    }

    private suspend fun complete(character: String) {
        val prefix = "A".repeat(LIMIT - character.encodeToByteArray().size) + character
        preview((prefix + "tail").encodeToByteArray(), prefix + NOTICE)
    }

    private suspend fun malformed(vararg octets: Int) {
        val prefix = "A".repeat(LIMIT - 1)
        preview(prefix.encodeToByteArray() + octets.map { it.toByte() }.toByteArray(), prefix + "\uFFFD" + NOTICE)
    }

    private suspend fun preview(bytes: ByteArray, expected: String) {
        val port = object : DownloadFileUseCase {
            override suspend fun invoke(itemId: String) = Result.Success(bytes)
            override fun getDownloadUrl(itemId: String): String = error("preview must use the domain download port")
        }
        assertEquals(expected, assertIs<Result.Success<String>>(loadTextPreview(port, "id")).data)
    }

    private companion object {
        const val LIMIT = 100 * 1024
        const val NOTICE = "\n\n... (truncated - file too large for preview)"
    }
}
