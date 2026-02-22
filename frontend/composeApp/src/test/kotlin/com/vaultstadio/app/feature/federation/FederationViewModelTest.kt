/**
 * Unit tests for FederationViewModel: loadInstances, loadShares, loadIdentities, loadActivities,
 * getInstanceDetails, clearSelectedInstance, clearError, requestFederation error path.
 */

package com.vaultstadio.app.feature.federation

import com.vaultstadio.app.domain.federation.model.FederatedActivity
import com.vaultstadio.app.domain.federation.model.FederatedActivityType
import com.vaultstadio.app.domain.federation.model.FederatedIdentity
import com.vaultstadio.app.domain.federation.model.FederatedInstance
import com.vaultstadio.app.domain.federation.model.FederatedShare
import com.vaultstadio.app.domain.federation.model.FederatedShareStatus
import com.vaultstadio.app.domain.federation.model.FederationCapability
import com.vaultstadio.app.domain.federation.model.InstanceStatus
import com.vaultstadio.app.domain.federation.model.SharePermission
import com.vaultstadio.app.domain.federation.usecase.AcceptFederatedShareUseCase
import com.vaultstadio.app.domain.federation.usecase.BlockInstanceUseCase
import com.vaultstadio.app.domain.federation.usecase.DeclineFederatedShareUseCase
import com.vaultstadio.app.domain.federation.usecase.GetFederatedActivitiesUseCase
import com.vaultstadio.app.domain.federation.usecase.GetFederatedIdentitiesUseCase
import com.vaultstadio.app.domain.federation.usecase.GetFederatedInstanceUseCase
import com.vaultstadio.app.domain.federation.usecase.GetFederatedInstancesUseCase
import com.vaultstadio.app.domain.federation.usecase.GetIncomingFederatedSharesUseCase
import com.vaultstadio.app.domain.federation.usecase.GetOutgoingFederatedSharesUseCase
import com.vaultstadio.app.domain.federation.usecase.LinkIdentityUseCase
import com.vaultstadio.app.domain.federation.usecase.RemoveInstanceUseCase
import com.vaultstadio.app.domain.federation.usecase.RequestFederationUseCase
import com.vaultstadio.app.domain.federation.usecase.RevokeFederatedShareUseCase
import com.vaultstadio.app.domain.federation.usecase.UnlinkIdentityUseCase
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.feature.ViewModelTestBase
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testFederatedInstance(
    id: String = "inst-1",
    domain: String = "remote.example.com",
) = FederatedInstance(
    id = id,
    domain = domain,
    name = "Remote",
    description = null,
    version = "1.0",
    capabilities = listOf(FederationCapability.RECEIVE_SHARES),
    status = InstanceStatus.ONLINE,
    lastSeenAt = testInstant,
    registeredAt = testInstant,
)

private class FakeGetFederatedInstancesUseCase(
    var result: Result<List<FederatedInstance>> = Result.success(emptyList()),
) : GetFederatedInstancesUseCase {
    override suspend fun invoke(status: InstanceStatus?): Result<List<FederatedInstance>> = result
}

private class FakeGetFederatedInstanceUseCase(
    var result: Result<FederatedInstance> = Result.error("NOT_FOUND", "Not found"),
) : GetFederatedInstanceUseCase {
    override suspend fun invoke(domain: String): Result<FederatedInstance> = result
}

private class FakeRequestFederationUseCase(
    var result: Result<FederatedInstance> = Result.success(testFederatedInstance()),
) : RequestFederationUseCase {
    override suspend fun invoke(targetDomain: String, message: String?): Result<FederatedInstance> = result
}

private class FakeBlockInstanceUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : BlockInstanceUseCase {
    override suspend fun invoke(instanceId: String): Result<Unit> = result
}

private class FakeRemoveInstanceUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : RemoveInstanceUseCase {
    override suspend fun invoke(instanceId: String): Result<Unit> = result
}

private class FakeGetOutgoingFederatedSharesUseCase(
    var result: Result<List<FederatedShare>> = Result.success(emptyList()),
) : GetOutgoingFederatedSharesUseCase {
    override suspend fun invoke(): Result<List<FederatedShare>> = result
}

private class FakeGetIncomingFederatedSharesUseCase(
    var result: Result<List<FederatedShare>> = Result.success(emptyList()),
) : GetIncomingFederatedSharesUseCase {
    override suspend fun invoke(status: FederatedShareStatus?): Result<List<FederatedShare>> = result
}

private class FakeAcceptFederatedShareUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : AcceptFederatedShareUseCase {
    override suspend fun invoke(shareId: String): Result<Unit> = result
}

private class FakeDeclineFederatedShareUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : DeclineFederatedShareUseCase {
    override suspend fun invoke(shareId: String): Result<Unit> = result
}

private class FakeRevokeFederatedShareUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : RevokeFederatedShareUseCase {
    override suspend fun invoke(shareId: String): Result<Unit> = result
}

private class FakeGetFederatedIdentitiesUseCase(
    var result: Result<List<FederatedIdentity>> = Result.success(emptyList()),
) : GetFederatedIdentitiesUseCase {
    override suspend fun invoke(): Result<List<FederatedIdentity>> = result
}

private class FakeLinkIdentityUseCase(
    var result: Result<FederatedIdentity> = Result.success(
        FederatedIdentity(
            id = "id-1",
            remoteUserId = "u1",
            remoteInstance = "r.example.com",
            displayName = "User",
            verified = false,
            linkedAt = testInstant,
        ),
    ),
) : LinkIdentityUseCase {
    override suspend fun invoke(
        remoteUserId: String,
        remoteInstance: String,
        displayName: String,
    ): Result<FederatedIdentity> = result
}

private class FakeUnlinkIdentityUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : UnlinkIdentityUseCase {
    override suspend fun invoke(identityId: String): Result<Unit> = result
}

private class FakeGetFederatedActivitiesUseCase(
    var result: Result<List<FederatedActivity>> = Result.success(emptyList()),
) : GetFederatedActivitiesUseCase {
    override suspend fun invoke(
        instance: String?,
        since: Instant?,
        limit: Int,
    ): Result<List<FederatedActivity>> = result
}

private fun testFederatedShare() = FederatedShare(
    id = "share-1",
    itemId = "item-1",
    sourceInstance = "local",
    targetInstance = "remote",
    targetUserId = null,
    permissions = listOf(SharePermission.READ),
    status = FederatedShareStatus.PENDING,
    expiresAt = testInstant,
    createdBy = "user-1",
    createdAt = testInstant,
    acceptedAt = null,
)

class FederationViewModelTest {

    private fun createViewModel(
        getInstancesResult: Result<List<FederatedInstance>> = Result.success(emptyList()),
        getInstanceResult: Result<FederatedInstance> = Result.error("NOT_FOUND", "Not found"),
    ): FederationViewModel = FederationViewModel(
        getFederatedInstancesUseCase = FakeGetFederatedInstancesUseCase(getInstancesResult),
        getFederatedInstanceUseCase = FakeGetFederatedInstanceUseCase(getInstanceResult),
        requestFederationUseCase = FakeRequestFederationUseCase(),
        blockInstanceUseCase = FakeBlockInstanceUseCase(),
        removeInstanceUseCase = FakeRemoveInstanceUseCase(),
        getOutgoingSharesUseCase = FakeGetOutgoingFederatedSharesUseCase(),
        getIncomingSharesUseCase = FakeGetIncomingFederatedSharesUseCase(),
        acceptShareUseCase = FakeAcceptFederatedShareUseCase(),
        declineShareUseCase = FakeDeclineFederatedShareUseCase(),
        revokeShareUseCase = FakeRevokeFederatedShareUseCase(),
        getIdentitiesUseCase = FakeGetFederatedIdentitiesUseCase(),
        linkIdentityUseCase = FakeLinkIdentityUseCase(),
        unlinkIdentityUseCase = FakeUnlinkIdentityUseCase(),
        getActivitiesUseCase = FakeGetFederatedActivitiesUseCase(),
    )

    @Test
    fun loadInstances_success_populatesInstances() = ViewModelTestBase.runTestWithMain {
        val instances = listOf(testFederatedInstance())
        val vm = createViewModel(getInstancesResult = Result.success(instances))
        vm.loadInstances()
        assertEquals(instances, vm.instances)
        assertNull(vm.error)
    }

    @Test
    fun loadInstances_error_setsError() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getInstancesResult = Result.error("ERR", "Load failed"))
        vm.loadInstances()
        assertTrue(vm.instances.isEmpty())
        assertEquals("Load failed", vm.error)
    }

    @Test
    fun loadShares_success_populatesOutgoingAndIncoming() = ViewModelTestBase.runTestWithMain {
        val outgoing = listOf(testFederatedShare())
        val getOutgoing = FakeGetOutgoingFederatedSharesUseCase(Result.success(outgoing))
        val getIncoming = FakeGetIncomingFederatedSharesUseCase(Result.success(emptyList()))
        val vm = FederationViewModel(
            getFederatedInstancesUseCase = FakeGetFederatedInstancesUseCase(),
            getFederatedInstanceUseCase = FakeGetFederatedInstanceUseCase(),
            requestFederationUseCase = FakeRequestFederationUseCase(),
            blockInstanceUseCase = FakeBlockInstanceUseCase(),
            removeInstanceUseCase = FakeRemoveInstanceUseCase(),
            getOutgoingSharesUseCase = getOutgoing,
            getIncomingSharesUseCase = getIncoming,
            acceptShareUseCase = FakeAcceptFederatedShareUseCase(),
            declineShareUseCase = FakeDeclineFederatedShareUseCase(),
            revokeShareUseCase = FakeRevokeFederatedShareUseCase(),
            getIdentitiesUseCase = FakeGetFederatedIdentitiesUseCase(),
            linkIdentityUseCase = FakeLinkIdentityUseCase(),
            unlinkIdentityUseCase = FakeUnlinkIdentityUseCase(),
            getActivitiesUseCase = FakeGetFederatedActivitiesUseCase(),
        )
        vm.loadShares()
        assertEquals(outgoing, vm.outgoingShares)
        assertTrue(vm.incomingShares.isEmpty())
    }

    @Test
    fun loadIdentities_success_populatesIdentities() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel()
        vm.loadIdentities()
        assertTrue(vm.identities.isEmpty())
        assertNull(vm.error)
    }

    @Test
    fun loadActivities_success_populatesActivities() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel()
        vm.loadActivities()
        assertTrue(vm.activities.isEmpty())
        assertNull(vm.error)
    }

    @Test
    fun getInstanceDetails_success_setsSelectedInstance() = ViewModelTestBase.runTestWithMain {
        val instance = testFederatedInstance(domain = "x.example.com")
        val getInstance = FakeGetFederatedInstanceUseCase(Result.success(instance))
        val vm = FederationViewModel(
            getFederatedInstancesUseCase = FakeGetFederatedInstancesUseCase(),
            getFederatedInstanceUseCase = getInstance,
            requestFederationUseCase = FakeRequestFederationUseCase(),
            blockInstanceUseCase = FakeBlockInstanceUseCase(),
            removeInstanceUseCase = FakeRemoveInstanceUseCase(),
            getOutgoingSharesUseCase = FakeGetOutgoingFederatedSharesUseCase(),
            getIncomingSharesUseCase = FakeGetIncomingFederatedSharesUseCase(),
            acceptShareUseCase = FakeAcceptFederatedShareUseCase(),
            declineShareUseCase = FakeDeclineFederatedShareUseCase(),
            revokeShareUseCase = FakeRevokeFederatedShareUseCase(),
            getIdentitiesUseCase = FakeGetFederatedIdentitiesUseCase(),
            linkIdentityUseCase = FakeLinkIdentityUseCase(),
            unlinkIdentityUseCase = FakeUnlinkIdentityUseCase(),
            getActivitiesUseCase = FakeGetFederatedActivitiesUseCase(),
        )
        vm.getInstanceDetails("x.example.com")
        assertEquals(instance, vm.selectedInstance)
    }

    @Test
    fun clearSelectedInstance_clearsSelection() = ViewModelTestBase.runTestWithMain {
        val instance = testFederatedInstance()
        val getInstance = FakeGetFederatedInstanceUseCase(Result.success(instance))
        val vm = FederationViewModel(
            getFederatedInstancesUseCase = FakeGetFederatedInstancesUseCase(),
            getFederatedInstanceUseCase = getInstance,
            requestFederationUseCase = FakeRequestFederationUseCase(),
            blockInstanceUseCase = FakeBlockInstanceUseCase(),
            removeInstanceUseCase = FakeRemoveInstanceUseCase(),
            getOutgoingSharesUseCase = FakeGetOutgoingFederatedSharesUseCase(),
            getIncomingSharesUseCase = FakeGetIncomingFederatedSharesUseCase(),
            acceptShareUseCase = FakeAcceptFederatedShareUseCase(),
            declineShareUseCase = FakeDeclineFederatedShareUseCase(),
            revokeShareUseCase = FakeRevokeFederatedShareUseCase(),
            getIdentitiesUseCase = FakeGetFederatedIdentitiesUseCase(),
            linkIdentityUseCase = FakeLinkIdentityUseCase(),
            unlinkIdentityUseCase = FakeUnlinkIdentityUseCase(),
            getActivitiesUseCase = FakeGetFederatedActivitiesUseCase(),
        )
        vm.getInstanceDetails("remote.example.com")
        assertTrue(vm.selectedInstance != null)
        vm.clearSelectedInstance()
        assertNull(vm.selectedInstance)
    }

    @Test
    fun clearError_clearsErrorMessage() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getInstancesResult = Result.error("ERR", "Oops"))
        vm.loadInstances()
        assertEquals("Oops", vm.error)
        vm.clearError()
        assertNull(vm.error)
    }

    @Test
    fun requestFederation_error_setsError() = ViewModelTestBase.runTestWithMain {
        val requestUseCase = FakeRequestFederationUseCase(Result.error("ERR", "Request failed"))
        val vm = FederationViewModel(
            getFederatedInstancesUseCase = FakeGetFederatedInstancesUseCase(),
            getFederatedInstanceUseCase = FakeGetFederatedInstanceUseCase(),
            requestFederationUseCase = requestUseCase,
            blockInstanceUseCase = FakeBlockInstanceUseCase(),
            removeInstanceUseCase = FakeRemoveInstanceUseCase(),
            getOutgoingSharesUseCase = FakeGetOutgoingFederatedSharesUseCase(),
            getIncomingSharesUseCase = FakeGetIncomingFederatedSharesUseCase(),
            acceptShareUseCase = FakeAcceptFederatedShareUseCase(),
            declineShareUseCase = FakeDeclineFederatedShareUseCase(),
            revokeShareUseCase = FakeRevokeFederatedShareUseCase(),
            getIdentitiesUseCase = FakeGetFederatedIdentitiesUseCase(),
            linkIdentityUseCase = FakeLinkIdentityUseCase(),
            unlinkIdentityUseCase = FakeUnlinkIdentityUseCase(),
            getActivitiesUseCase = FakeGetFederatedActivitiesUseCase(),
        )
        vm.requestFederation("remote.com", null)
        assertEquals("Request failed", vm.error)
    }
}
