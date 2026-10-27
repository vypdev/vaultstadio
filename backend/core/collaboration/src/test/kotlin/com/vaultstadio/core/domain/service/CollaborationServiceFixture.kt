package com.vaultstadio.core.domain.service

import arrow.core.right
import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.core.domain.model.CommentAnchor
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.core.domain.model.DocumentState
import com.vaultstadio.core.domain.model.UserPresence
import com.vaultstadio.core.domain.repository.CollaborationRepository
import com.vaultstadio.domain.collaboration.repository.CollaborationAdmission
import com.vaultstadio.domain.collaboration.repository.CollaborationMembershipRepository
import com.vaultstadio.domain.collaboration.repository.CollaborationScopeRepository
import com.vaultstadio.domain.collaboration.repository.CollaborationSessionScope
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours

internal class CollaborationServiceFixture {
    val repository = mockk<CollaborationRepository>()
    val items = mockk<StorageItemRepository>()
    val scopes = mockk<CollaborationScopeRepository>()
    val membership = mockk<CollaborationMembershipRepository>()
    val service = CollaborationService(repository, items, scopes, membership)
    val now = Clock.System.now()
    var item =
        StorageItem(
            "item-1",
            "file.txt",
            "/file.txt",
            ItemType.FILE,
            ownerId = "user-1",
            createdAt = now,
            updatedAt = now,
        )
    val participant = CollaborationParticipant("p1", "user-1", "John", "#FF6B6B", joinedAt = now, lastActiveAt = now)
    var session =
        CollaborationSession(
            "session-1",
            item.id,
            participants = listOf(participant),
            createdAt = now,
            expiresAt =
            now + 24.hours,
        )
    var existing: CollaborationSession? = null
    val state = DocumentState(item.id, 1, "before", lastModified = now)
    val operation = CollaborationOperation.Insert(
        userId = "user-1",
        timestamp = now,
        baseVersion = 1,
        position = 0,
        text = "after",
    )
    val comment =
        DocumentComment(
            "comment-1",
            item.id,
            "user-1",
            "review",
            CommentAnchor(0, 0, 1, 1, "quote"),
            createdAt = now,
            updatedAt = now,
        )

    init {
        coEvery { items.findById(any()) } answers { item.right() }
        coEvery { scopes.session(any()) } answers { CollaborationSessionScope(item.id, false).right() }
        coEvery { scopes.participantSession(any()) } answers { session.id.right() }
        coEvery { scopes.commentItem(any()) } answers { item.id.right() }
        coEvery { repository.findSession(any()) } answers { session.right() }
        coEvery { membership.admit(any(), any(), any(), any()) } answers {
            val plan = arg<CollaborationAdmission>(3)(existing)
            plan.map { (planned, participant) ->
                session = planned.copy(participants = planned.participants + participant)
                Pair(session, participant)
            }
        }
        coEvery { membership.leave(any(), any(), any(), any(), any()) } returns Unit.right()
        coEvery { membership.update(any(), any(), any(), any(), any()) } answers
            { arg<CollaborationParticipant>(2).right() }
        coEvery { membership.closeExpiredForOwner(any(), any()) } returns 0.right()
        coEvery { repository.getDocumentState(any()) } returns state.right()
        coEvery { repository.applyOperation(any(), any()) } returns
            state.copy(version = 2, content = "afterbefore").right()
        coEvery { repository.getOperationsSince(any(), any()) } returns listOf(operation).right()
        coEvery { repository.saveDocumentState(any()) } answers { firstArg<DocumentState>().right() }
        coEvery { repository.createComment(any()) } answers { firstArg<DocumentComment>().right() }
        coEvery { repository.getCommentsForItem(any(), any()) } returns listOf(comment).right()
        coEvery { repository.findComment(any()) } returns comment.right()
        coEvery { repository.resolveComment(any(), any()) } returns comment.copy(resolvedAt = now).right()
        coEvery { repository.addReply(any(), any()) } answers { comment.copy(replies = listOf(secondArg())).right() }
        coEvery { repository.deleteComment(any()) } returns Unit.right()
        coEvery { repository.setOffline(any()) } returns Unit.right()
        coEvery { repository.updatePresence(any()) } answers { firstArg<UserPresence>().right() }
        every { repository.streamOperations(any()) } returns flowOf(operation)
        every { repository.streamComments(any()) } returns flowOf(comment)
    }
}
