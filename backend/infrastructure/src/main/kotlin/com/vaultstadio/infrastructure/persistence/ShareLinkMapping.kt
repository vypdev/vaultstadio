package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.infrastructure.persistence.entities.ShareLinksTable
import org.jetbrains.exposed.v1.core.ResultRow

internal fun ResultRow.toShareLinkModel() = ShareLink(
    id = this[ShareLinksTable.id],
    itemId = this[ShareLinksTable.itemId],
    token = this[ShareLinksTable.token],
    createdBy = this[ShareLinksTable.createdBy],
    expiresAt = this[ShareLinksTable.expiresAt],
    password = this[ShareLinksTable.password],
    maxDownloads = this[ShareLinksTable.maxDownloads],
    downloadCount = this[ShareLinksTable.downloadCount],
    isActive = this[ShareLinksTable.isActive],
    createdAt = this[ShareLinksTable.createdAt],
    sharedWithUsers = this[ShareLinksTable.sharedWithUsers].split(",").filter { it.isNotBlank() },
)
