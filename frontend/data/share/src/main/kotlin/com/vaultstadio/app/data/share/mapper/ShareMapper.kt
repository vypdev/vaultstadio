/**
 * Share mappers: DTO to domain.
 */

package com.vaultstadio.app.data.share.mapper

import com.vaultstadio.app.data.share.dto.CreateShareRequestDTO
import com.vaultstadio.app.data.share.dto.ShareLinkDTO
import com.vaultstadio.app.domain.share.model.ShareLink
import kotlin.time.Instant
import kotlinx.datetime.Instant as DateTimeInstant

fun ShareLinkDTO.toDomain(): ShareLink = ShareLink(
    id = id,
    itemId = itemId,
    token = token,
    url = url,
    expiresAt = expiresAt?.toDateTimeInstant(),
    hasPassword = hasPassword,
    maxDownloads = maxDownloads,
    downloadCount = downloadCount,
    isActive = isActive,
    createdAt = createdAt.toDateTimeInstant(),
    createdBy = createdBy,
    sharedWithUsers = sharedWithUsers,
)

private fun Instant.toDateTimeInstant(): DateTimeInstant =
    DateTimeInstant.fromEpochMilliseconds(toEpochMilliseconds())

fun List<ShareLinkDTO>.toShareList(): List<ShareLink> = map { it.toDomain() }

fun createShareRequestDTO(
    itemId: String,
    expiresInDays: Int?,
    password: String?,
    maxDownloads: Int?,
): CreateShareRequestDTO = CreateShareRequestDTO(
    itemId = itemId,
    expiresInDays = expiresInDays,
    password = password,
    maxDownloads = maxDownloads,
)
