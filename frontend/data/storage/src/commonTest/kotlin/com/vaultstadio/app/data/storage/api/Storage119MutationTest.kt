package com.vaultstadio.app.data.storage.api

import com.vaultstadio.app.data.storage.dto.CopyRequestDTO
import com.vaultstadio.app.data.storage.dto.CreateFolderRequestDTO
import com.vaultstadio.app.data.storage.dto.MoveRequestDTO
import com.vaultstadio.app.data.storage.dto.RenameRequestDTO
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class Storage119MutationTest {
    @Test fun sto119CreateFolderPreservesNameAndParent() = runTest {
        storage119Request(HttpMethod.Post, "/api/v1/storage/folder", body = """{"name":"é + &","parentId":"parent119"}""") {
            it.createFolder(CreateFolderRequestDTO("é + &", "parent119"))
        }
    }
    @Test fun sto119RenameUsesPatchAndNewName() = runTest {
        storage119Request(HttpMethod.Patch, "/api/v1/storage/item/file119/rename", body = """{"name":"renamed.txt"}""") {
            it.renameItem("file119", RenameRequestDTO("renamed.txt"))
        }
    }
    @Test fun sto119MoveRetainsNullRootDestinationAndOptionalName() = runTest {
        storage119Request(HttpMethod.Post, "/api/v1/storage/item/file119/move", body = """{"destinationId":null,"newName":"moved.txt"}""") {
            it.moveItem("file119", MoveRequestDTO(null, "moved.txt"))
        }
    }
    @Test fun sto119CopyRetainsDestinationWithoutInventingRename() = runTest {
        storage119Request(HttpMethod.Post, "/api/v1/storage/item/file119/copy", body = """{"destinationId":"dest119","newName":null}""") {
            it.copyItem("file119", CopyRequestDTO("dest119"))
        }
    }
    @Test fun sto119StarUsesNoBodyCommand() = runTest {
        storage119Request(HttpMethod.Post, "/api/v1/storage/item/file119/star") { it.toggleStar("file119") }
    }
    @Test fun sto119TrashIsDistinctFromPermanentDelete() = runTest {
        storage119Request(HttpMethod.Delete, "/api/v1/storage/item/file119") { it.trashItem("file119") }
    }
    @Test fun sto119PermanentDeleteReturnsUnitWithPermanentFlag() = runTest {
        storage119Request(HttpMethod.Delete, "/api/v1/storage/item/file119", STORAGE119_UNIT, mapOf("permanent" to "true")) {
            it.deleteItemPermanently("file119")
        }
    }
    @Test fun sto119RestoreUsesOwnCommandPath() = runTest {
        storage119Request(HttpMethod.Post, "/api/v1/storage/item/file119/restore") { it.restoreItem("file119") }
    }
}
