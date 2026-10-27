@file:OptIn(ExperimentalCoroutinesApi::class)

package com.vaultstadio.app.feature.files

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

internal class Files119Harness {
    val loading = Files119LoadingPorts()
    val mutation = Files119MutationPorts()
    val extra = Files119ExtraPorts()
    val preferences = Files119Preferences()
    val models = mutableListOf<FilesViewModel>()
    fun model(mode: FilesMode = FilesMode.ALL) = FilesViewModel(
        preferences, loading.folder, loading.recent, loading.starred, loading.trash, loading.breadcrumbs,
        mutation.create, mutation.rename, mutation.star, mutation.trash, mutation.restore, mutation.delete,
        mutation.empty, mutation.batchDelete, mutation.batchMove, mutation.batchCopy, mutation.batchStar,
        extra.search, extra.suggestions, extra.activity, extra.share, extra.urls, extra.shareUrl, mode,
    ).also { models += it }
}
internal fun files119Test(block: suspend TestScope.(Files119Harness) -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val fixture = Files119Harness()
    try {
        block(fixture)
    } finally {
        fixture.models.forEach { it.viewModelScope.cancel() }
        runCurrent()
        Dispatchers.resetMain()
    }
}
