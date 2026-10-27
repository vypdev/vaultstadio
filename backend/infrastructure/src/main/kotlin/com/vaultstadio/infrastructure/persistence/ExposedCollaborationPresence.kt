package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.core.domain.model.PresenceStatus
import com.vaultstadio.core.domain.model.UserPresence
import com.vaultstadio.core.domain.repository.CollaborationPresenceRepository
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.infrastructure.persistence.entities.UserPresenceTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

internal class ExposedCollaborationPresence : CollaborationPresenceRepository {
    override suspend fun updatePresence(presence: UserPresence): Either<StorageException, UserPresence> {
        return collaborationRepositoryCall("Failed to update presence") {
            collaborationDbQuery {
                val exists = UserPresenceTable.selectAll()
                    .where { UserPresenceTable.userId eq presence.userId }
                    .count() > 0

                if (exists) {
                    UserPresenceTable.update({ UserPresenceTable.userId eq presence.userId }) {
                        it[status] = presence.status.name
                        it[lastSeen] = presence.lastSeen
                        it[activeSession] = presence.activeSession
                        it[activeDocument] = presence.activeDocument
                    }
                } else {
                    UserPresenceTable.insert {
                        it[userId] = presence.userId
                        it[status] = presence.status.name
                        it[lastSeen] = presence.lastSeen
                        it[activeSession] = presence.activeSession
                        it[activeDocument] = presence.activeDocument
                    }
                }
            }
            presence.right()
        }
    }

    override suspend fun getPresence(userId: String): Either<StorageException, UserPresence?> {
        return collaborationRepositoryCall("Failed to get presence") {
            collaborationDbQuery {
                UserPresenceTable.selectAll()
                    .where { UserPresenceTable.userId eq userId }
                    .map { it.toUserPresence() }
                    .singleOrNull()
            }.right()
        }
    }

    override suspend fun getPresenceBulk(userIds: List<String>): Either<StorageException, Map<String, UserPresence>> {
        return collaborationRepositoryCall("Failed to get presence bulk") {
            collaborationDbQuery {
                UserPresenceTable.selectAll()
                    .where { UserPresenceTable.userId inList userIds }
                    .associate { it[UserPresenceTable.userId] to it.toUserPresence() }
            }.right()
        }
    }

    override suspend fun setOffline(userId: String): Either<StorageException, Unit> {
        return collaborationRepositoryCall("Failed to set offline") {
            collaborationDbQuery {
                UserPresenceTable.update({ UserPresenceTable.userId eq userId }) {
                    it[status] = PresenceStatus.OFFLINE.name
                    it[lastSeen] = Clock.System.now()
                    it[activeSession] = null
                    it[activeDocument] = null
                }
            }
            Unit.right()
        }
    }

    override suspend fun getOnlineUsersInFolder(folderId: String?): Either<StorageException, List<UserPresence>> {
        return collaborationRepositoryCall("Failed to get online users") {
            collaborationDbQuery {
                UserPresenceTable.selectAll()
                    .where { UserPresenceTable.status eq PresenceStatus.ONLINE.name }
                    .map { it.toUserPresence() }
            }.right()
        }
    }

    override fun streamPresence(userIds: List<String>): Flow<UserPresence> = flow {
        // Real-time streaming would use a different mechanism
    }
}
