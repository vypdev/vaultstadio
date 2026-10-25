/**
 * Unit tests for federation use cases (GetFederatedInstances, GetFederatedInstance, etc.).
 * Uses a fake FederationRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.data.federation.usecase.AcceptFederatedShareUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.BlockInstanceUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.CreateFederatedShareUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.DeclineFederatedShareUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.GetFederatedActivitiesUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.GetFederatedIdentitiesUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.GetFederatedInstanceUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.GetFederatedInstancesUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.GetIncomingFederatedSharesUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.GetOutgoingFederatedSharesUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.LinkIdentityUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.RemoveInstanceUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.RequestFederationUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.RevokeFederatedShareUseCaseImpl
import com.vaultstadio.app.data.federation.usecase.UnlinkIdentityUseCaseImpl
import com.vaultstadio.app.domain.federation.FederationRepository
import com.vaultstadio.app.domain.federation.model.FederatedActivity
import com.vaultstadio.app.domain.federation.model.FederatedActivityType
import com.vaultstadio.app.domain.federation.model.FederatedIdentity
import com.vaultstadio.app.domain.federation.model.FederatedInstance
import com.vaultstadio.app.domain.federation.model.FederatedShare
import com.vaultstadio.app.domain.federation.model.FederatedShareStatus
import com.vaultstadio.app.domain.federation.model.InstanceStatus
import com.vaultstadio.app.domain.federation.model.SharePermission
import com.vaultstadio.app.domain.result.Result
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testFederatedInstance(
    id: String = "inst-1",
    domain: String = "storage.example.com",
    name: String = "Example Storage",
    status: InstanceStatus = InstanceStatus.ONLINE,
) = FederatedInstance(
    id = id,
    domain = domain,
    name = name,
    description = null,
    version = "1.0",
    capabilities = emptyList(),
    status = status,
    lastSeenAt = null,
    registeredAt = testInstant,
)

private fun testFederatedShare(
    id: String = "share-1",
    itemId: String = "item-1",
    sourceInstance: String = "remote.example.com",
    status: FederatedShareStatus = FederatedShareStatus.PENDING,
) = FederatedShare(
    id = id,
    itemId = itemId,
    sourceInstance = sourceInstance,
    targetInstance = "local.example.com",
    targetUserId = null,
    permissions = listOf(SharePermission.READ),
    status = status,
    expiresAt = null,
    createdBy = "remote-user",
    createdAt = testInstant,
    acceptedAt = null,
)

private fun testFederatedIdentity(
    id: String = "ident-1",
    remoteUserId: String = "remote-u1",
    remoteInstance: String = "remote.example.com",
    displayName: String = "Remote User",
) = FederatedIdentity(
    id = id,
    localUserId = null,
    remoteUserId = remoteUserId,
    remoteInstance = remoteInstance,
    displayName = displayName,
    email = null,
    avatarUrl = null,
    verified = false,
    linkedAt = testInstant,
)

private fun testFederatedActivity(
    id: String = "act-1",
    instanceDomain: String = "remote.example.com",
    activityType: FederatedActivityType = FederatedActivityType.FILE_ACCESSED,
) = FederatedActivity(
    id = id,
    instanceDomain = instanceDomain,
    activityType = activityType,
    actorId = "user-1",
    objectId = "item-1",
    objectType = "file",
    summary = "Accessed",
    timestamp = testInstant,
)

private fun <T> stubResult(): Result<T> = Result.error("TEST", "Not implemented in fake")

private class FakeFederationRepository(
    var getInstancesResult: Result<List<FederatedInstance>> = Result.success(emptyList()),
    var getInstanceResult: Result<FederatedInstance> = Result.success(testFederatedInstance()),
    var requestFederationResult: Result<FederatedInstance> = Result.success(testFederatedInstance()),
    var blockInstanceResult: Result<Unit> = Result.success(Unit),
    var removeInstanceResult: Result<Unit> = Result.success(Unit),
    var getIncomingSharesResult: Result<List<FederatedShare>> = Result.success(emptyList()),
    var getOutgoingSharesResult: Result<List<FederatedShare>> = Result.success(emptyList()),
    var acceptShareResult: Result<Unit> = Result.success(Unit),
    var declineShareResult: Result<Unit> = Result.success(Unit),
    var revokeShareResult: Result<Unit> = Result.success(Unit),
    var getIdentitiesResult: Result<List<FederatedIdentity>> = Result.success(emptyList()),
    var getActivitiesResult: Result<List<FederatedActivity>> = Result.success(emptyList()),
    var createShareResult: Result<FederatedShare> = Result.success(testFederatedShare()),
    var linkIdentityResult: Result<FederatedIdentity> = Result.success(testFederatedIdentity()),
    var unlinkIdentityResult: Result<Unit> = Result.success(Unit),
) : FederationRepository {

    override suspend fun requestFederation(targetDomain: String, message: String?): Result<FederatedInstance> =
        requestFederationResult

    override suspend fun getInstances(status: InstanceStatus?): Result<List<FederatedInstance>> = getInstancesResult

    override suspend fun getInstance(domain: String): Result<FederatedInstance> = getInstanceResult

    override suspend fun blockInstance(instanceId: String): Result<Unit> = blockInstanceResult

    override suspend fun removeInstance(instanceId: String): Result<Unit> = removeInstanceResult

    override suspend fun createShare(
        itemId: String,
        targetInstance: String,
        targetUserId: String?,
        permissions: List<SharePermission>,
        expiresInDays: Int?,
    ): Result<FederatedShare> = createShareResult

    override suspend fun getOutgoingShares(): Result<List<FederatedShare>> = getOutgoingSharesResult

    override suspend fun getIncomingShares(status: FederatedShareStatus?): Result<List<FederatedShare>> =
        getIncomingSharesResult

    override suspend fun acceptShare(shareId: String): Result<Unit> = acceptShareResult

    override suspend fun declineShare(shareId: String): Result<Unit> = declineShareResult

    override suspend fun revokeShare(shareId: String): Result<Unit> = revokeShareResult

    override suspend fun linkIdentity(
        remoteUserId: String,
        remoteInstance: String,
        displayName: String,
    ): Result<FederatedIdentity> = linkIdentityResult

    override suspend fun getIdentities(): Result<List<FederatedIdentity>> = getIdentitiesResult

    override suspend fun unlinkIdentity(identityId: String): Result<Unit> = unlinkIdentityResult

    override suspend fun getActivities(
        instance: String?,
        since: Instant?,
        limit: Int,
    ): Result<List<FederatedActivity>> = getActivitiesResult
}

class GetFederatedInstancesUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetInstancesResult() = runTest {
        val instances = listOf(
            testFederatedInstance("i1", "a.com", "Instance A", InstanceStatus.ONLINE),
            testFederatedInstance("i2", "b.com", "Instance B", InstanceStatus.OFFLINE),
        )
        val repo = FakeFederationRepository(getInstancesResult = Result.success(instances))
        val useCase = GetFederatedInstancesUseCaseImpl(repo)
        val result = useCase(status = null)
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("a.com", result.getOrNull()?.get(0)?.domain)
    }

    @Test
    fun invoke_withStatus_forwardsToRepository() = runTest {
        val instances = listOf(testFederatedInstance(status = InstanceStatus.ONLINE))
        val repo = FakeFederationRepository(getInstancesResult = Result.success(instances))
        val useCase = GetFederatedInstancesUseCaseImpl(repo)
        val result = useCase(InstanceStatus.ONLINE)
        assertTrue(result.isSuccess())
        assertEquals(InstanceStatus.ONLINE, result.getOrNull()?.get(0)?.status)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(getInstancesResult = Result.error("FORBIDDEN", "Admin only"))
        val useCase = GetFederatedInstancesUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class GetFederatedInstanceUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetInstanceResult() = runTest {
        val instance = testFederatedInstance(domain = "storage.example.com", name = "Example")
        val repo = FakeFederationRepository(getInstanceResult = Result.success(instance))
        val useCase = GetFederatedInstanceUseCaseImpl(repo)
        val result = useCase("storage.example.com")
        assertTrue(result.isSuccess())
        assertEquals(instance, result.getOrNull())
        assertEquals("storage.example.com", result.getOrNull()?.domain)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(getInstanceResult = Result.error("NOT_FOUND", "Instance not found"))
        val useCase = GetFederatedInstanceUseCaseImpl(repo)
        val result = useCase("unknown.domain")
        assertTrue(result.isError())
    }
}

class RequestFederationUseCaseTest {

    @Test
    fun invoke_returnsRepositoryRequestFederationResult() = runTest {
        val instance = testFederatedInstance(domain = "new.example.com", status = InstanceStatus.PENDING)
        val repo = FakeFederationRepository(requestFederationResult = Result.success(instance))
        val useCase = RequestFederationUseCaseImpl(repo)
        val result = useCase("new.example.com", message = "Hello")
        assertTrue(result.isSuccess())
        assertEquals("new.example.com", result.getOrNull()?.domain)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(requestFederationResult = Result.error("INVALID", "Invalid domain"))
        val useCase = RequestFederationUseCaseImpl(repo)
        val result = useCase("bad.domain")
        assertTrue(result.isError())
    }
}

class BlockInstanceUseCaseTest {

    @Test
    fun invoke_returnsRepositoryBlockInstanceResult() = runTest {
        val repo = FakeFederationRepository(blockInstanceResult = Result.success(Unit))
        val useCase = BlockInstanceUseCaseImpl(repo)
        val result = useCase("instance-1")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(blockInstanceResult = Result.error("NOT_FOUND", "Instance not found"))
        val useCase = BlockInstanceUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
    }
}

class GetIncomingFederatedSharesUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetIncomingSharesResult() = runTest {
        val shares = listOf(
            testFederatedShare("s1", "item-1", "remote.com", FederatedShareStatus.PENDING),
            testFederatedShare("s2", "item-2", "other.com", FederatedShareStatus.ACCEPTED),
        )
        val repo = FakeFederationRepository(getIncomingSharesResult = Result.success(shares))
        val useCase = GetIncomingFederatedSharesUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("item-1", result.getOrNull()?.get(0)?.itemId)
        assertEquals(FederatedShareStatus.ACCEPTED, result.getOrNull()?.get(1)?.status)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(getIncomingSharesResult = Result.error("UNAUTHORIZED", "Not logged in"))
        val useCase = GetIncomingFederatedSharesUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class GetOutgoingFederatedSharesUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetOutgoingSharesResult() = runTest {
        val shares = listOf(
            testFederatedShare("out-1", "item-1", "local", FederatedShareStatus.ACCEPTED),
        )
        val repo = FakeFederationRepository(getOutgoingSharesResult = Result.success(shares))
        val useCase = GetOutgoingFederatedSharesUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("out-1", result.getOrNull()?.get(0)?.id)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(getOutgoingSharesResult = Result.error("FORBIDDEN", "Admin only"))
        val useCase = GetOutgoingFederatedSharesUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class RemoveInstanceUseCaseTest {

    @Test
    fun invoke_returnsRepositoryRemoveInstanceResult() = runTest {
        val repo = FakeFederationRepository(removeInstanceResult = Result.success(Unit))
        val useCase = RemoveInstanceUseCaseImpl(repo)
        val result = useCase("instance-1")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(removeInstanceResult = Result.error("NOT_FOUND", "Instance not found"))
        val useCase = RemoveInstanceUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
    }
}

class AcceptFederatedShareUseCaseTest {

    @Test
    fun invoke_returnsRepositoryAcceptShareResult() = runTest {
        val repo = FakeFederationRepository(acceptShareResult = Result.success(Unit))
        val useCase = AcceptFederatedShareUseCaseImpl(repo)
        val result = useCase("share-1")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(acceptShareResult = Result.error("NOT_FOUND", "Share not found"))
        val useCase = AcceptFederatedShareUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
    }
}

class DeclineFederatedShareUseCaseTest {

    @Test
    fun invoke_returnsRepositoryDeclineShareResult() = runTest {
        val repo = FakeFederationRepository(declineShareResult = Result.success(Unit))
        val useCase = DeclineFederatedShareUseCaseImpl(repo)
        val result = useCase("share-1")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(declineShareResult = Result.error("NOT_FOUND", "Share not found"))
        val useCase = DeclineFederatedShareUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
    }
}

class RevokeFederatedShareUseCaseTest {

    @Test
    fun invoke_returnsRepositoryRevokeShareResult() = runTest {
        val repo = FakeFederationRepository(revokeShareResult = Result.success(Unit))
        val useCase = RevokeFederatedShareUseCaseImpl(repo)
        val result = useCase("share-1")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(revokeShareResult = Result.error("FORBIDDEN", "Cannot revoke"))
        val useCase = RevokeFederatedShareUseCaseImpl(repo)
        val result = useCase("share-1")
        assertTrue(result.isError())
    }
}

class GetFederatedIdentitiesUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetIdentitiesResult() = runTest {
        val identities = listOf(testFederatedIdentity("id-1"), testFederatedIdentity("id-2"))
        val repo = FakeFederationRepository(getIdentitiesResult = Result.success(identities))
        val useCase = GetFederatedIdentitiesUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("id-1", result.getOrNull()?.get(0)?.id)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(getIdentitiesResult = Result.error("UNAUTHORIZED", "Not logged in"))
        val useCase = GetFederatedIdentitiesUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class GetFederatedActivitiesUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetActivitiesResult() = runTest {
        val activities = listOf(testFederatedActivity("a1"), testFederatedActivity("a2"))
        val repo = FakeFederationRepository(getActivitiesResult = Result.success(activities))
        val useCase = GetFederatedActivitiesUseCaseImpl(repo)
        val result = useCase(instance = null, since = null, limit = 50)
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("a1", result.getOrNull()?.get(0)?.id)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(getActivitiesResult = Result.error("FORBIDDEN", "Access denied"))
        val useCase = GetFederatedActivitiesUseCaseImpl(repo)
        val result = useCase(instance = "remote.com", since = testInstant, limit = 10)
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class CreateFederatedShareUseCaseTest {

    @Test
    fun invoke_returnsRepositoryCreateShareResult() = runTest {
        val share = testFederatedShare("created-1", "item-1", "target.example.com", FederatedShareStatus.PENDING)
        val repo = FakeFederationRepository(createShareResult = Result.success(share))
        val useCase = CreateFederatedShareUseCaseImpl(repo)
        val result = useCase(
            itemId = "item-1",
            targetInstance = "target.example.com",
            targetUserId = null,
            permissions = listOf(SharePermission.READ),
            expiresInDays = 7,
        )
        assertTrue(result.isSuccess())
        assertEquals(share, result.getOrNull())
        assertEquals("created-1", result.getOrNull()?.id)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(createShareResult = Result.error("FORBIDDEN", "Cannot share"))
        val useCase = CreateFederatedShareUseCaseImpl(repo)
        val result = useCase("item-1", "target.com", null, emptyList(), null)
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class LinkIdentityUseCaseTest {

    @Test
    fun invoke_returnsRepositoryLinkIdentityResult() = runTest {
        val identity = testFederatedIdentity("linked-1", "remote-u", "r.example.com", "Remote User")
        val repo = FakeFederationRepository(linkIdentityResult = Result.success(identity))
        val useCase = LinkIdentityUseCaseImpl(repo)
        val result = useCase("remote-u", "r.example.com", "Remote User")
        assertTrue(result.isSuccess())
        assertEquals(identity, result.getOrNull())
        assertEquals("linked-1", result.getOrNull()?.id)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(linkIdentityResult = Result.error("CONFLICT", "Already linked"))
        val useCase = LinkIdentityUseCaseImpl(repo)
        val result = useCase("user", "instance.com", "Display")
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class UnlinkIdentityUseCaseTest {

    @Test
    fun invoke_returnsRepositoryUnlinkIdentityResult() = runTest {
        val repo = FakeFederationRepository(unlinkIdentityResult = Result.success(Unit))
        val useCase = UnlinkIdentityUseCaseImpl(repo)
        val result = useCase("identity-1")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(unlinkIdentityResult = Result.error("NOT_FOUND", "Identity not found"))
        val useCase = UnlinkIdentityUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
    }
}
