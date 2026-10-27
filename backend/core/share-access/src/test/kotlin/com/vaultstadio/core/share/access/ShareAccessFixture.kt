package com.vaultstadio.core.share.access

import arrow.core.right
import com.vaultstadio.domain.share.access.PreparedShareContent
import com.vaultstadio.domain.share.access.PublicShareAccessRepository
import com.vaultstadio.domain.share.access.PublicShareSnapshot
import com.vaultstadio.domain.share.access.ShareContentReader
import com.vaultstadio.domain.share.access.SharePasswordVerifier
import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Instant

internal class ShareAccessFixture {
    val now = Instant.parse("2026-10-27T00:00:00Z")
    var share = ShareLink(itemId = "item", token = "ToKen", createdBy = "owner", createdAt = now)
    val item = StorageItem(
        id = "item", name = "file", path = "/private/file", type = ItemType.FILE,
        ownerId = "owner", size = 5, storageKey = "private-key", createdAt = now, updatedAt = now,
    )
    val repository = mockk<PublicShareAccessRepository>()
    val passwords = mockk<SharePasswordVerifier>()
    val reader = mockk<ShareContentReader>()
    var closed = false
    val prepared = object : PreparedShareContent {
        override val stream = "bytes".byteInputStream()
        override fun close() {
            closed = true
            stream.close()
        }
    }
    val service = PublicShareService(repository, passwords, reader) { now }

    init {
        coEvery { repository.find(any()) } answers { share.right() }
        coEvery { repository.inspect(any()) } answers { PublicShareSnapshot(share, item).right() }
        coEvery { repository.admit(any()) } returns Unit.right()
        coEvery { reader.prepare(any()) } returns prepared.right()
        every { passwords.matches(any(), any()) } answers { firstArg<String>() == "correct" }
    }
}
