@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.vaultstadio.app.feature.shares

import com.vaultstadio.app.domain.config.usecase.GetShareUrlUseCase
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.share.ShareLinkClipboard
import com.vaultstadio.app.domain.share.model.ShareLink
import com.vaultstadio.app.domain.share.usecase.DeleteShareUseCase
import com.vaultstadio.app.domain.share.usecase.GetMySharesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant

internal fun clipboard116Test(block: suspend TestScope.() -> Unit) = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}

internal fun clipboard116Model(writer: ShareLinkClipboard) = SharesViewModel(
    object : GetMySharesUseCase {
        override suspend fun invoke() = Result.Success(emptyList<ShareLink>())
    },
    object : DeleteShareUseCase {
        override suspend fun invoke(shareId: String) = Result.Success(Unit)
    },
    object : GetShareUrlUseCase {
        override fun invoke(token: String) = "https://viewer.invalid/gateway/api/v1/share/$token/view"
    },
    writer,
)

internal fun clipboard116Share(token: String = "CaseToken_-") = ShareLink(
    id = "share116", itemId = "item116", token = token, url = "http://internal.invalid/wrong-root",
    expiresAt = null, hasPassword = false, maxDownloads = null, downloadCount = 0, isActive = true,
    createdAt = Instant.fromEpochMilliseconds(0),
)
