package com.vaultstadio.app.feature.files

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.BatchResult
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.storage.usecase.BatchCopyUseCase
import com.vaultstadio.app.domain.storage.usecase.BatchDeleteUseCase
import com.vaultstadio.app.domain.storage.usecase.BatchMoveUseCase
import com.vaultstadio.app.domain.storage.usecase.BatchStarUseCase
import com.vaultstadio.app.domain.storage.usecase.CreateFolderUseCase
import com.vaultstadio.app.domain.storage.usecase.DeleteItemUseCase
import com.vaultstadio.app.domain.storage.usecase.EmptyTrashUseCase
import com.vaultstadio.app.domain.storage.usecase.RenameItemUseCase
import com.vaultstadio.app.domain.storage.usecase.RestoreItemUseCase
import com.vaultstadio.app.domain.storage.usecase.ToggleStarUseCase
import com.vaultstadio.app.domain.storage.usecase.TrashItemUseCase

internal data class Files119Mutation(val operation: String, val arguments: List<Any?>)
internal class Files119MutationPorts {
    val calls = mutableListOf<Files119Mutation>()
    var itemResult: Result<StorageItem> = Result.Success(files119Item("changed119"))
    var batchResult: Result<BatchResult> = Result.Success(BatchResult(1, 0))
    var unitResult: Result<Unit> = Result.Success(Unit)
    var beforeItem: suspend () -> Unit = {}
    var beforeBatch: suspend () -> Unit = {}
    private fun record(operation: String, vararg args: Any?) {
        calls += Files119Mutation(operation, args.toList())
    }
    val create = object : CreateFolderUseCase {
        override suspend fun invoke(name: String, parentId: String?): Result<StorageItem> {
            record("create", name, parentId)
            beforeItem()
            return itemResult
        }
    }
    val rename = object : RenameItemUseCase {
        override suspend fun invoke(itemId: String, newName: String): Result<StorageItem> {
            record("rename", itemId, newName)
            beforeItem()
            return itemResult
        }
    }
    val star = object : ToggleStarUseCase {
        override suspend fun invoke(itemId: String): Result<StorageItem> {
            record("star", itemId)
            beforeItem()
            return itemResult
        }
    }
    val trash = object : TrashItemUseCase {
        override suspend fun invoke(itemId: String): Result<StorageItem> {
            record("trash", itemId)
            beforeItem()
            return itemResult
        }
    }
    val restore = object : RestoreItemUseCase {
        override suspend fun invoke(itemId: String): Result<StorageItem> {
            record("restore", itemId)
            beforeItem()
            return itemResult
        }
    }
    val delete = object : DeleteItemUseCase {
        override suspend fun invoke(itemId: String): Result<Unit> {
            record("delete", itemId)
            return unitResult
        }
    }
    val empty = object : EmptyTrashUseCase {
        override suspend fun invoke(): Result<BatchResult> {
            record("empty")
            beforeBatch()
            return batchResult
        }
    }
    val batchDelete = object : BatchDeleteUseCase {
        override suspend fun invoke(itemIds: List<String>, permanent: Boolean): Result<BatchResult> {
            record("batchDelete", itemIds, permanent)
            beforeBatch()
            return batchResult
        }
    }
    val batchMove = object : BatchMoveUseCase {
        override suspend fun invoke(itemIds: List<String>, destinationId: String?): Result<BatchResult> {
            record("batchMove", itemIds, destinationId)
            beforeBatch()
            return batchResult
        }
    }
    val batchCopy = object : BatchCopyUseCase {
        override suspend fun invoke(itemIds: List<String>, destinationId: String?): Result<BatchResult> {
            record("batchCopy", itemIds, destinationId)
            beforeBatch()
            return batchResult
        }
    }
    val batchStar = object : BatchStarUseCase {
        override suspend fun invoke(itemIds: List<String>, starred: Boolean): Result<BatchResult> {
            record("batchStar", itemIds, starred)
            beforeBatch()
            return batchResult
        }
    }
}
