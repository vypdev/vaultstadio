/**
 * Unit tests for [ShareLink] (isExpired, isDownloadLimitReached).
 */

package com.vaultstadio.domain.share.model

import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShareLinkTest {

    private val now = Instant.fromEpochMilliseconds(1_600_000_000_000)
    private val future = Instant.fromEpochMilliseconds(1_700_000_000_000)
    private val past = Instant.fromEpochMilliseconds(1_500_000_000_000)

    @Test
    fun isExpiredReturnsTrueWhenExpiresAtBeforeNow() {
        val share = ShareLink(
            id = "s1",
            itemId = "i1",
            token = "t1",
            createdBy = "u1",
            expiresAt = past,
            createdAt = now,
        )
        assertTrue(share.isExpired(now))
    }

    @Test
    fun isExpiredReturnsFalseWhenExpiresAtAfterNow() {
        val share = ShareLink(
            id = "s1",
            itemId = "i1",
            token = "t1",
            createdBy = "u1",
            expiresAt = future,
            createdAt = now,
        )
        assertFalse(share.isExpired(now))
    }

    @Test
    fun isExpiredReturnsFalseWhenExpiresAtIsNull() {
        val share = ShareLink(
            id = "s1",
            itemId = "i1",
            token = "t1",
            createdBy = "u1",
            expiresAt = null,
            createdAt = now,
        )
        assertFalse(share.isExpired(now))
    }

    @Test
    fun isDownloadLimitReachedReturnsTrueWhenDownloadCountGeMax() {
        val share = ShareLink(
            id = "s1",
            itemId = "i1",
            token = "t1",
            createdBy = "u1",
            maxDownloads = 2,
            downloadCount = 2,
            createdAt = now,
        )
        assertTrue(share.isDownloadLimitReached())
    }

    @Test
    fun isDownloadLimitReachedReturnsFalseWhenBelowLimit() {
        val share = ShareLink(
            id = "s1",
            itemId = "i1",
            token = "t1",
            createdBy = "u1",
            maxDownloads = 5,
            downloadCount = 2,
            createdAt = now,
        )
        assertFalse(share.isDownloadLimitReached())
    }

    @Test
    fun isDownloadLimitReachedReturnsFalseWhenMaxDownloadsNull() {
        val share = ShareLink(
            id = "s1",
            itemId = "i1",
            token = "t1",
            createdBy = "u1",
            maxDownloads = null,
            downloadCount = 100,
            createdAt = now,
        )
        assertFalse(share.isDownloadLimitReached())
    }
}
