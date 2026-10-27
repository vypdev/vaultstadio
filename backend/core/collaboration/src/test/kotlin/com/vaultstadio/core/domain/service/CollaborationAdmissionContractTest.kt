package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.domain.collaboration.repository.CollaborationAdmission
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.InvalidOperationException
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertIs

class CollaborationAdmissionContractTest {
    @ParameterizedTest(name = "C89-ADMISSION {0}")
    @ValueSource(
        strings = [
            "wrong-existing-item",
            "blank-existing-id",
            "duplicate-existing-participant",
            "expired-existing",
            "unplanned-success",
            "wrong-returned-session",
            "wrong-returned-participant",
            "wrong-returned-user",
            "unbound-returned-participant",
        ],
    )
    fun `admission validates both the locked plan and the returned identities`(case: String) = runTest {
        val f = CollaborationServiceFixture()
        arrange(f, case)
        val result = f.service.joinSession(JoinSessionInput(f.item.id), "user-1", "Owner")
        val error = assertIs<Either.Left<*>>(result).value
        if (case ==
            "expired-existing"
        ) {
            assertIs<InvalidOperationException>(error)
        } else {
            assertIs<DatabaseException>(error)
        }
    }

    private fun arrange(f: CollaborationServiceFixture, case: String) {
        when (case) {
            "wrong-existing-item" -> f.existing = f.session.copy(itemId = "foreign")
            "blank-existing-id" -> f.existing = f.session.copy(id = " ")
            "duplicate-existing-participant" ->
                f.existing =
                    f.session.copy(participants = listOf(f.participant, f.participant))
            "expired-existing" -> f.existing = f.session.copy(expiresAt = f.now)
            "unplanned-success" -> coEvery { f.membership.admit(any(), any(), any(), any()) } returns
                Pair(f.session, f.participant).right()
            else -> coEvery { f.membership.admit(any(), any(), any(), any()) } answers {
                arg<CollaborationAdmission>(3)(null).map { (session, participant) ->
                    val bound = session.copy(participants = listOf(participant))
                    when (case) {
                        "wrong-returned-session" -> Pair(bound.copy(id = "foreign"), participant)
                        "wrong-returned-participant" -> Pair(bound, participant.copy(id = "foreign"))
                        "wrong-returned-user" -> Pair(bound, participant.copy(userId = "foreign"))
                        "unbound-returned-participant" -> Pair(bound.copy(participants = emptyList()), participant)
                        else -> error("Unenumerated result corruption")
                    }
                }
            }
        }
    }
}
