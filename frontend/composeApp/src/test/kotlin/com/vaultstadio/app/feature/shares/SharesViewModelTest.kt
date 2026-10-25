/**
 * Unit tests for SharesViewModel: loadShares, copyLink, clearClipboardLink, deleteShare, clearError.
 */

package com.vaultstadio.app.feature.shares

import com.vaultstadio.app.domain.config.usecase.GetShareUrlUseCase
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.share.model.ShareLink
import com.vaultstadio.app.domain.share.usecase.DeleteShareUseCase
import com.vaultstadio.app.domain.share.usecase.GetMySharesUseCase
import com.vaultstadio.app.feature.ViewModelTestBase
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testShareLink(id: String = "share-1", token: String = "tok123") = ShareLink(
    id = id,
    itemId = "item-1",
    token = token,
    url = "https://example.com/share/$token",
    expiresAt = testInstant,
    hasPassword = false,
    maxDownloads = 10,
    downloadCount = 0,
    isActive = true,
    createdAt = testInstant,
    createdBy = "user-1",
    sharedWithUsers = emptyList(),
)

private class FakeGetMySharesUseCase(
    var result: Result<List<ShareLink>> = Result.success(emptyList()),
) : GetMySharesUseCase {
    override suspend fun invoke(): Result<List<ShareLink>> = result
}

private class FakeDeleteShareUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : DeleteShareUseCase {
    override suspend fun invoke(shareId: String): Result<Unit> = result
}

private class FakeGetShareUrlUseCase(
    var url: String = "https://app.test/share/",
) : GetShareUrlUseCase {
    override fun invoke(token: String): String = url + token
}

class SharesViewModelTest {

    private fun createViewModel(
        getMySharesResult: Result<List<ShareLink>> = Result.success(emptyList()),
        deleteShareResult: Result<Unit> = Result.success(Unit),
        getShareUrl: (String) -> String = { "https://app.test/share/$it" },
    ): SharesViewModel = SharesViewModel(
        getMySharesUseCase = FakeGetMySharesUseCase(getMySharesResult),
        deleteShareUseCase = FakeDeleteShareUseCase(deleteShareResult),
        getShareUrlUseCase = object : GetShareUrlUseCase {
            override fun invoke(token: String): String = getShareUrl(token)
        },
    )

    @Test
    fun loadShares_success_populatesShares() = ViewModelTestBase.runTestWithMain {
        val links = listOf(testShareLink("s1", "t1"), testShareLink("s2", "t2"))
        val vm = createViewModel(getMySharesResult = Result.success(links))
        vm.loadShares()
        assertEquals(links, vm.shares)
        assertNull(vm.error)
    }

    @Test
    fun loadShares_error_setsError() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getMySharesResult = Result.error("ERR", "Load failed"))
        vm.loadShares()
        assertTrue(vm.shares.isEmpty())
        assertEquals("Load failed", vm.error)
    }

    @Test
    fun copyLink_setsClipboardLinkFromGetShareUrl() = ViewModelTestBase.runTestWithMain {
        val share = testShareLink(token = "abc")
        val vm = createViewModel(getShareUrl = { "https://copy.me/$it" })
        vm.copyLink(share)
        assertEquals("https://copy.me/abc", vm.clipboardLink)
    }

    @Test
    fun clearClipboardLink_clearsClipboardLink() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel()
        vm.copyLink(testShareLink())
        assertTrue(vm.clipboardLink != null)
        vm.clearClipboardLink()
        assertNull(vm.clipboardLink)
    }

    @Test
    fun deleteShare_success_reloadsShares() = ViewModelTestBase.runTestWithMain {
        val links = listOf(testShareLink("s1"))
        val getUseCase = FakeGetMySharesUseCase(Result.success(links))
        val vm = SharesViewModel(
            getMySharesUseCase = getUseCase,
            deleteShareUseCase = FakeDeleteShareUseCase(Result.success(Unit)),
            getShareUrlUseCase = FakeGetShareUrlUseCase(),
        )
        vm.loadShares()
        assertEquals(1, vm.shares.size)
        getUseCase.result = Result.success(emptyList<ShareLink>())
        vm.deleteShare(links.first())
        assertEquals(emptyList<ShareLink>(), vm.shares)
        assertNull(vm.error)
    }

    @Test
    fun deleteShare_error_setsError() = ViewModelTestBase.runTestWithMain {
        val share = testShareLink()
        val vm = createViewModel(
            getMySharesResult = Result.success(listOf(share)),
            deleteShareResult = Result.error("ERR", "Delete failed"),
        )
        vm.loadShares()
        vm.deleteShare(share)
        assertEquals("Delete failed", vm.error)
    }

    @Test
    fun clearError_clearsErrorMessage() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getMySharesResult = Result.error("ERR", "Oops"))
        vm.loadShares()
        assertEquals("Oops", vm.error)
        vm.clearError()
        assertNull(vm.error)
    }
}
