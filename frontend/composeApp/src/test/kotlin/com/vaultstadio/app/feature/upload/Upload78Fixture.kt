@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.vaultstadio.app.feature.upload

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.ItemType
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.storage.model.Visibility
import com.vaultstadio.app.domain.upload.UploadQueueEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Instant

internal data class Upload78Call(
    val name: String,
    val bytes: ByteArray,
    val mime: String,
    val parent: String?,
    val progress: (Float) -> Unit,
)

internal open class Upload78Repository : StubStorageRepository() {
    val calls = mutableListOf<Upload78Call>()
    var transfer: suspend (Upload78Call) -> Result<StorageItem> = { Result.Success(upload78Item(it.name, it.parent)) }
    val folders = mutableListOf<Pair<String, String?>>()
    var folder: suspend (String, String?) -> Result<StorageItem> = { name, parent ->
        Result.Success(upload78Item(name, parent, folder = true))
    }

    override suspend fun createFolder(name: String, parentId: String?): Result<StorageItem> {
        folders += name to parentId
        return folder(name, parentId)
    }

    override suspend fun uploadFile(
        fileName: String,
        fileData: ByteArray,
        mimeType: String,
        parentId: String?,
        onProgress: (Float) -> Unit,
    ): Result<StorageItem> {
        val call = Upload78Call(fileName, fileData.copyOf(), mimeType, parentId, onProgress)
        calls += call
        return transfer(call)
    }
}

internal fun upload78Item(name: String, parent: String? = null, folder: Boolean = false) = StorageItem(
    name, name, "/$name", if (folder) ItemType.FOLDER else ItemType.FILE, parent, 0,
    "application/octet-stream", Visibility.PRIVATE, false, false,
    Instant.fromEpochMilliseconds(0), Instant.fromEpochMilliseconds(0),
)

internal class Upload78Fixture(val scope: TestScope) {
    val repository = Upload78Repository()
    val manager = UploadManager(repository, StandardTestDispatcher(scope.testScheduler))
    var completions = 0

    init {
        scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) {
            manager.uploadCompleted.collect { completions++ }
        }
    }

    fun add(name: String = "archivo ñ.bin", bytes: ByteArray = byteArrayOf(0, 127, -1), parent: String? = "folder78") {
        manager.addEntries(listOf(UploadQueueEntry.WithData(name, bytes.size.toLong(), "test/binary", bytes)), parent)
    }

    fun tick() = scope.runCurrent()
}

internal fun upload78Test(test: suspend Upload78Fixture.() -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val fixture = Upload78Fixture(this)
    try {
        fixture.test()
    } finally {
        fixture.manager.clear()
        runCurrent()
        Dispatchers.resetMain()
    }
}
