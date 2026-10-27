package com.vaultstadio.domain.share.access

import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import kotlinx.datetime.Instant
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class PublicSharePolicyTest {
    private val now = Instant.parse("2026-10-27T00:00:00Z")
    private val share = ShareLink(itemId = "item", token = "ToKen", createdBy = "owner", createdAt = now)
    private val item = StorageItem(
        id = "item", name = "file", path = "/file", type = ItemType.FILE, ownerId = "owner",
        size = 5, storageKey = "private-key", createdAt = now, updatedAt = now,
    )

    @ParameterizedTest(name = "S90-policy-link {0}")
    @ValueSource(
        strings = [
            "active",
            "revoked",
            "past",
            "equal",
            "future",
            "negative",
            "zero-limit",
            "bad-limit",
            "last",
            "full",
            "over",
            "overflow",
        ],
    )
    fun linkStates(case: String) {
        val candidate = when (case) {
            "revoked" -> share.copy(isActive = false)
            "past" -> share.copy(expiresAt = Instant.fromEpochSeconds(now.epochSeconds - 1))
            "equal" -> share.copy(expiresAt = now)
            "future" -> share.copy(expiresAt = Instant.fromEpochSeconds(now.epochSeconds + 1))
            "negative" -> share.copy(downloadCount = -1)
            "zero-limit" -> share.copy(maxDownloads = 0)
            "bad-limit" -> share.copy(maxDownloads = -1)
            "last" -> share.copy(downloadCount = 1, maxDownloads = 2)
            "full" -> share.copy(downloadCount = 2, maxDownloads = 2)
            "over" -> share.copy(downloadCount = 3, maxDownloads = 2)
            "overflow" -> share.copy(downloadCount = Int.MAX_VALUE)
            else -> share
        }
        val expected = when (case) {
            "revoked" -> PublicShareFailure.REVOKED
            "past", "equal" -> PublicShareFailure.EXPIRED
            "negative", "zero-limit", "bad-limit" -> PublicShareFailure.NOT_FOUND
            "full", "over", "overflow" -> PublicShareFailure.LIMIT_REACHED
            else -> null
        }
        assertEquals(expected, PublicSharePolicy.linkFailure(candidate, now))
    }

    @ParameterizedTest(name = "S90-policy-item {0}")
    @ValueSource(
        strings = [
            "valid",
            "missing",
            "other-item",
            "other-owner",
            "folder",
            "trash",
            "no-key",
            "blank-key",
            "negative-size",
            "empty-file",
        ],
    )
    fun resourceAuthority(case: String) {
        val candidate = when (case) {
            "missing" -> null
            "other-item" -> item.copy(id = "other")
            "other-owner" -> item.copy(ownerId = "other")
            "folder" -> item.copy(type = ItemType.FOLDER)
            "trash" -> item.copy(isTrashed = true)
            "no-key" -> item.copy(storageKey = null)
            "blank-key" -> item.copy(storageKey = " ")
            "negative-size" -> item.copy(size = -1)
            "empty-file" -> item.copy(size = 0)
            else -> item
        }
        assertEquals(
            if (case in listOf("valid", "empty-file")) null else PublicShareFailure.NOT_FOUND,
            PublicSharePolicy.itemFailure(share, candidate),
        )
    }

    @ParameterizedTest(name = "S90-policy-credential {0}")
    @ValueSource(strings = ["same", "counter", "state", "id", "token-case", "item", "creator", "hash"])
    fun credentialBinding(case: String) {
        val changed = when (case) {
            "counter" -> share.copy(downloadCount = 1)
            "state" -> share.copy(isActive = false)
            "id" -> share.copy(id = "other")
            "token-case" -> share.copy(token = "token")
            "item" -> share.copy(itemId = "other")
            "creator" -> share.copy(createdBy = "other")
            "hash" -> share.copy(password = "changed")
            else -> share
        }
        assertEquals(case in listOf("same", "counter", "state"), PublicSharePolicy.sameCredential(share, changed))
    }

    @ParameterizedTest(name = "S90-policy-creation {0}")
    @ValueSource(
        strings = [
            "defaults",
            "no-expiry",
            "one-day",
            "max-days",
            "zero-days",
            "negative-days",
            "too-long",
            "one-download",
            "max-downloads",
            "zero-downloads",
            "negative-downloads",
            "empty-password",
            "one-byte",
            "72-bytes",
            "73-bytes",
            "72-utf8",
            "74-utf8",
            "surrogate-high",
            "surrogate-low",
        ],
    )
    fun creationBounds(case: String) {
        val days = when (case) {
            "no-expiry" -> null
            "one-day" -> 1
            "max-days" -> 365
            "zero-days" -> 0
            "negative-days" -> -1
            "too-long" -> 366
            else -> 7
        }
        val limit = when (case) {
            "one-download" -> 1
            "max-downloads" -> Int.MAX_VALUE
            "zero-downloads" -> 0
            "negative-downloads" -> -1
            else -> null
        }
        val password = when (case) {
            "empty-password" -> ""
            "one-byte" -> "a"
            "72-bytes" -> "a".repeat(72)
            "73-bytes" -> "a".repeat(73)
            "72-utf8" -> "é".repeat(36)
            "74-utf8" -> "é".repeat(37)
            "surrogate-high" -> "\uD800"
            "surrogate-low" -> "\uDC00"
            else -> null
        }
        val denied = case in listOf(
            "zero-days",
            "negative-days",
            "too-long",
            "zero-downloads",
            "negative-downloads",
            "empty-password",
            "73-bytes",
            "74-utf8",
            "surrogate-high",
            "surrogate-low",
        )
        assertEquals(!denied, PublicSharePolicy.validCreation(days, limit, password))
    }
}
