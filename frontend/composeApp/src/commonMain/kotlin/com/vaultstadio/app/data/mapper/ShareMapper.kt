/**
 * Share Mappers
 */

package com.vaultstadio.app.data.mapper

import com.vaultstadio.app.data.dto.share.CreateShareRequestDTO
import com.vaultstadio.app.data.dto.share.ShareLinkDTO
import com.vaultstadio.app.domain.model.ShareLink

fun ShareLinkDTO.toDomain(): ShareLink = ShareLink(
    id = id,
    itemId = itemId,
    token = token,
    url = url,
    expiresAt = expiresAt,
    hasPassword = hasPassword,
    maxDownloads = maxDownloads,
    downloadCount = downloadCount,
    isActive = isActive,
    createdAt = createdAt,
    createdBy = createdBy,
    sharedWithUsers = sharedWithUsers,
)

fun List<ShareLinkDTO>.toShareList(): List<ShareLink> = map { it.toDomain() }

fun createShareRequestDTO(
    itemId: String,
    expiresInDays: Int?,
    password: String?,
    maxDownloads: Int?,
): CreateShareRequestDTO = CreateShareRequestDTO(itemId, expiresInDays, password, maxDownloads)
