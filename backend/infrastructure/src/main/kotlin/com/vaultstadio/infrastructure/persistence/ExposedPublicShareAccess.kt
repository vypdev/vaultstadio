package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.share.access.PublicShareAccessRepository
import com.vaultstadio.domain.share.access.PublicShareClock
import com.vaultstadio.domain.share.access.PublicShareFailure
import com.vaultstadio.domain.share.access.PublicSharePolicy
import com.vaultstadio.domain.share.access.PublicShareSnapshot
import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.infrastructure.persistence.entities.ShareLinksTable
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import java.sql.SQLException

/** Locked authority and content identity are checked in the same transaction as the counter write. */
class ExposedPublicShareAccess(private val clock: PublicShareClock) : PublicShareAccessRepository {
    override suspend fun find(token: String): Either<PublicShareFailure, ShareLink?> = query {
        ShareLinksTable.selectAll().where { ShareLinksTable.token eq token }
            .singleOrNull()?.toShareLinkModel().right()
    }

    override suspend fun inspect(expected: ShareLink): Either<PublicShareFailure, PublicShareSnapshot> = query {
        lockedSnapshot(expected)
    }

    override suspend fun admit(expected: PublicShareSnapshot): Either<PublicShareFailure, Unit> = query {
        lockedSnapshot(expected.share).flatMap { current ->
            if (current.item != expected.item) {
                PublicShareFailure.CHANGED.left()
            } else {
                val count = current.share.downloadCount + 1
                val updated = ShareLinksTable.update({ ShareLinksTable.id eq current.share.id }) {
                    it[downloadCount] = count
                }
                check(updated == 1) { "Missing locked share" }
                Unit.right()
            }
        }
    }

    private fun lockedSnapshot(expected: ShareLink): Either<PublicShareFailure, PublicShareSnapshot> {
        val item = StorageItemsTable.selectAll().where { StorageItemsTable.id eq expected.itemId }
            .forUpdate().singleOrNull()?.toStorageItemModel()
        val share = ShareLinksTable.selectAll().where { ShareLinksTable.id eq expected.id }
            .forUpdate().singleOrNull()?.toShareLinkModel() ?: return PublicShareFailure.NOT_FOUND.left()
        if (!PublicSharePolicy.sameCredential(expected, share)) return PublicShareFailure.CHANGED.left()
        val failure = PublicSharePolicy.linkFailure(share, clock.now()) ?: PublicSharePolicy.itemFailure(share, item)
        return if (failure == null) PublicShareSnapshot(share, checkNotNull(item)).right() else failure.left()
    }
}

private suspend fun <T> query(operation: () -> Either<PublicShareFailure, T>): Either<PublicShareFailure, T> = try {
    newSuspendedTransaction(Dispatchers.IO) {
        maxAttempts = 1
        try {
            operation()
        } catch (_: SQLException) {
            // Roll back inside the transaction: Exposed's retry logger must never expand
            // capability/password query arguments into an exception log.
            rollback()
            PublicShareFailure.UNAVAILABLE.left()
        } catch (_: IllegalArgumentException) {
            rollback()
            PublicShareFailure.UNAVAILABLE.left()
        }
    }
} catch (_: SQLException) {
    PublicShareFailure.UNAVAILABLE.left()
} catch (_: IllegalArgumentException) {
    PublicShareFailure.UNAVAILABLE.left()
}
