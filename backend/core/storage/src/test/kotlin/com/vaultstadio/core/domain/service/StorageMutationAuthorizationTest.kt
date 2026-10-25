package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.storage.model.Visibility
import com.vaultstadio.domain.storage.repository.StorageItemQuery
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertIs

class StorageMutationAuthorizationTest {
    @TestFactory
    fun `all ordinary item operations deny foreign sources`() = Visibility.entries.flatMap { visibility ->
        listOf("read", "download", "list", "breadcrumbs", "rename", "move", "copy", "toggle-star", "set-star", "trash", "restore", "delete").map { operation ->
            DynamicTest.dynamicTest("$operation denies $visibility foreign source") {
                runTest {
                    val f = StorageAuthorizationFixture()
                    coEvery { f.repository.findById("foreign") } returns item("foreign", FOREIGN, visibility).right()
                    val result = when (operation) {
                        "read" -> f.service.getItem("foreign", ACTOR)
                        "download" -> f.service.downloadFile("foreign", ACTOR)
                        "list" -> f.service.listFolder("foreign", ACTOR, StorageItemQuery())
                        "breadcrumbs" -> f.service.getBreadcrumbs("foreign", ACTOR)
                        "rename" -> f.service.renameItem("foreign", "new", ACTOR)
                        "move" -> f.service.moveItem(MoveItemInput("foreign", null, userId = ACTOR))
                        "copy" -> f.service.copyItem(CopyItemInput("foreign", null, userId = ACTOR))
                        "toggle-star" -> f.service.toggleStar("foreign", ACTOR)
                        "set-star" -> f.service.setStar("foreign", ACTOR, true)
                        "trash" -> f.service.trashItem("foreign", ACTOR)
                        "restore" -> f.service.restoreItem("foreign", ACTOR)
                        else -> f.service.deleteItem("foreign", ACTOR)
                    }
                    assertIs<AuthorizationException>(assertIs<Either.Left<*>>(result).value)
                    f.assertNoEffects()
                }
            }
        }
    }
}
