/**
 * Unit tests for federation use cases (GetFederatedInstances, GetFederatedInstance).
 * Uses a fake FederationRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.FederationRepository
import com.vaultstadio.app.domain.model.FederatedActivity
import com.vaultstadio.app.domain.model.FederatedIdentity
import com.vaultstadio.app.domain.model.FederatedInstance
import com.vaultstadio.app.domain.model.FederatedShare
import com.vaultstadio.app.domain.model.FederatedShareStatus
import com.vaultstadio.app.domain.model.InstanceStatus
import com.vaultstadio.app.domain.model.SharePermission
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

private fun <T> stubResult(): ApiResult<T> = ApiResult.error("TEST", "Not implemented in fake")

private class FakeFederationRepository(
    var getInstancesResult: ApiResult<List<FederatedInstance>> = ApiResult.success(emptyList()),
    var getInstanceResult: ApiResult<FederatedInstance> = ApiResult.success(testFederatedInstance()),
    var requestFederationResult: ApiResult<FederatedInstance> = ApiResult.success(testFederatedInstance()),
    var blockInstanceResult: ApiResult<Unit> = ApiResult.success(Unit),
    var getIncomingSharesResult: ApiResult<List<FederatedShare>> = ApiResult.success(emptyList()),
) : FederationRepository {

    override suspend fun requestFederation(targetDomain: String, message: String?): ApiResult<FederatedInstance> =
        requestFederationResult

    override suspend fun getInstances(status: InstanceStatus?): ApiResult<List<FederatedInstance>> = getInstancesResult

    override suspend fun getInstance(domain: String): ApiResult<FederatedInstance> = getInstanceResult

    override suspend fun blockInstance(instanceId: String): ApiResult<Unit> = blockInstanceResult

    override suspend fun removeInstance(instanceId: String): ApiResult<Unit> = stubResult()

    override suspend fun createShare(
        itemId: String,
        targetInstance: String,
        targetUserId: String?,
        permissions: List<SharePermission>,
        expiresInDays: Int?,
    ): ApiResult<FederatedShare> = stubResult()

    override suspend fun getOutgoingShares(): ApiResult<List<FederatedShare>> = stubResult()

    override suspend fun getIncomingShares(status: FederatedShareStatus?): ApiResult<List<FederatedShare>> =
        getIncomingSharesResult

    override suspend fun acceptShare(shareId: String): ApiResult<Unit> = stubResult()

    override suspend fun declineShare(shareId: String): ApiResult<Unit> = stubResult()

    override suspend fun revokeShare(shareId: String): ApiResult<Unit> = stubResult()

    override suspend fun linkIdentity(
        remoteUserId: String,
        remoteInstance: String,
        displayName: String,
    ): ApiResult<FederatedIdentity> = stubResult()

    override suspend fun getIdentities(): ApiResult<List<FederatedIdentity>> = stubResult()

    override suspend fun unlinkIdentity(identityId: String): ApiResult<Unit> = stubResult()

    override suspend fun getActivities(
        instance: String?,
        since: Instant?,
        limit: Int,
    ): ApiResult<List<FederatedActivity>> = stubResult()
}

class GetFederatedInstancesUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetInstancesResult() = runTest {
        val instances = listOf(
            testFederatedInstance("i1", "a.com", "Instance A", InstanceStatus.ONLINE),
            testFederatedInstance("i2", "b.com", "Instance B", InstanceStatus.OFFLINE),
        )
        val repo = FakeFederationRepository(getInstancesResult = ApiResult.success(instances))
        val useCase = GetFederatedInstancesUseCaseImpl(repo)
        val result = useCase(status = null)
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("a.com", result.getOrNull()?.get(0)?.domain)
    }

    @Test
    fun invoke_withStatus_forwardsToRepository() = runTest {
        val instances = listOf(testFederatedInstance(status = InstanceStatus.ONLINE))
        val repo = FakeFederationRepository(getInstancesResult = ApiResult.success(instances))
        val useCase = GetFederatedInstancesUseCaseImpl(repo)
        val result = useCase(InstanceStatus.ONLINE)
        assertTrue(result.isSuccess())
        assertEquals(InstanceStatus.ONLINE, result.getOrNull()?.get(0)?.status)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(getInstancesResult = ApiResult.error("FORBIDDEN", "Admin only"))
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
        val repo = FakeFederationRepository(getInstanceResult = ApiResult.success(instance))
        val useCase = GetFederatedInstanceUseCaseImpl(repo)
        val result = useCase("storage.example.com")
        assertTrue(result.isSuccess())
        assertEquals(instance, result.getOrNull())
        assertEquals("storage.example.com", result.getOrNull()?.domain)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(getInstanceResult = ApiResult.error("NOT_FOUND", "Instance not found"))
        val useCase = GetFederatedInstanceUseCaseImpl(repo)
        val result = useCase("unknown.domain")
        assertTrue(result.isError())
    }
}

class RequestFederationUseCaseTest {

    @Test
    fun invoke_returnsRepositoryRequestFederationResult() = runTest {
        val instance = testFederatedInstance(domain = "new.example.com", status = InstanceStatus.PENDING)
        val repo = FakeFederationRepository(requestFederationResult = ApiResult.success(instance))
        val useCase = RequestFederationUseCaseImpl(repo)
        val result = useCase("new.example.com", message = "Hello")
        assertTrue(result.isSuccess())
        assertEquals("new.example.com", result.getOrNull()?.domain)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(requestFederationResult = ApiResult.error("INVALID", "Invalid domain"))
        val useCase = RequestFederationUseCaseImpl(repo)
        val result = useCase("bad.domain")
        assertTrue(result.isError())
    }
}

class BlockInstanceUseCaseTest {

    @Test
    fun invoke_returnsRepositoryBlockInstanceResult() = runTest {
        val repo = FakeFederationRepository(blockInstanceResult = ApiResult.success(Unit))
        val useCase = BlockInstanceUseCaseImpl(repo)
        val result = useCase("instance-1")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(blockInstanceResult = ApiResult.error("NOT_FOUND", "Instance not found"))
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
        val repo = FakeFederationRepository(getIncomingSharesResult = ApiResult.success(shares))
        val useCase = GetIncomingFederatedSharesUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("item-1", result.getOrNull()?.get(0)?.itemId)
        assertEquals(FederatedShareStatus.ACCEPTED, result.getOrNull()?.get(1)?.status)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeFederationRepository(getIncomingSharesResult = ApiResult.error("UNAUTHORIZED", "Not logged in"))
        val useCase = GetIncomingFederatedSharesUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}
