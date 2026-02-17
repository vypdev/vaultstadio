package com.vaultstadio.app.feature.federation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.model.FederatedActivity
import com.vaultstadio.app.domain.model.FederatedIdentity
import com.vaultstadio.app.domain.model.FederatedInstance
import com.vaultstadio.app.domain.model.FederatedShare
import com.vaultstadio.app.domain.usecase.federation.AcceptFederatedShareUseCase
import com.vaultstadio.app.domain.usecase.federation.BlockInstanceUseCase
import com.vaultstadio.app.domain.usecase.federation.DeclineFederatedShareUseCase
import com.vaultstadio.app.domain.usecase.federation.GetFederatedActivitiesUseCase
import com.vaultstadio.app.domain.usecase.federation.GetFederatedIdentitiesUseCase
import com.vaultstadio.app.domain.usecase.federation.GetFederatedInstanceUseCase
import com.vaultstadio.app.domain.usecase.federation.GetFederatedInstancesUseCase
import com.vaultstadio.app.domain.usecase.federation.GetIncomingFederatedSharesUseCase
import com.vaultstadio.app.domain.usecase.federation.GetOutgoingFederatedSharesUseCase
import com.vaultstadio.app.domain.usecase.federation.LinkIdentityUseCase
import com.vaultstadio.app.domain.usecase.federation.RemoveInstanceUseCase
import com.vaultstadio.app.domain.usecase.federation.RequestFederationUseCase
import com.vaultstadio.app.domain.usecase.federation.RevokeFederatedShareUseCase
import com.vaultstadio.app.domain.usecase.federation.UnlinkIdentityUseCase
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

/**
 * ViewModel for federation management.
 */
@KoinViewModel
class FederationViewModel(
    private val getFederatedInstancesUseCase: GetFederatedInstancesUseCase,
    private val getFederatedInstanceUseCase: GetFederatedInstanceUseCase,
    private val requestFederationUseCase: RequestFederationUseCase,
    private val blockInstanceUseCase: BlockInstanceUseCase,
    private val removeInstanceUseCase: RemoveInstanceUseCase,
    private val getOutgoingSharesUseCase: GetOutgoingFederatedSharesUseCase,
    private val getIncomingSharesUseCase: GetIncomingFederatedSharesUseCase,
    private val acceptShareUseCase: AcceptFederatedShareUseCase,
    private val declineShareUseCase: DeclineFederatedShareUseCase,
    private val revokeShareUseCase: RevokeFederatedShareUseCase,
    private val getIdentitiesUseCase: GetFederatedIdentitiesUseCase,
    private val linkIdentityUseCase: LinkIdentityUseCase,
    private val unlinkIdentityUseCase: UnlinkIdentityUseCase,
    private val getActivitiesUseCase: GetFederatedActivitiesUseCase,
) : ViewModel() {

    var instances by mutableStateOf<List<FederatedInstance>>(emptyList())
        private set
    var outgoingShares by mutableStateOf<List<FederatedShare>>(emptyList())
        private set
    var incomingShares by mutableStateOf<List<FederatedShare>>(emptyList())
        private set
    var identities by mutableStateOf<List<FederatedIdentity>>(emptyList())
        private set
    var activities by mutableStateOf<List<FederatedActivity>>(emptyList())
        private set
    var selectedInstance by mutableStateOf<FederatedInstance?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadInstances()
        loadShares()
    }

    fun loadInstances() {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = getFederatedInstancesUseCase()) {
                is ApiResult.Success -> instances = result.data
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
            isLoading = false
        }
    }

    fun loadShares() {
        viewModelScope.launch {
            error = null
            when (val outgoing = getOutgoingSharesUseCase()) {
                is ApiResult.Success -> outgoingShares = outgoing.data
                is ApiResult.Error -> error = outgoing.message
                is ApiResult.NetworkError -> error = outgoing.message
            }
            when (val incoming = getIncomingSharesUseCase()) {
                is ApiResult.Success -> incomingShares = incoming.data
                is ApiResult.Error -> if (error == null) error = incoming.message
                is ApiResult.NetworkError -> if (error == null) error = incoming.message
            }
        }
    }

    fun loadIdentities() {
        viewModelScope.launch {
            error = null
            when (val result = getIdentitiesUseCase()) {
                is ApiResult.Success -> identities = result.data
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun loadActivities() {
        viewModelScope.launch {
            error = null
            when (val result = getActivitiesUseCase()) {
                is ApiResult.Success -> activities = result.data
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun requestFederation(domain: String, message: String?) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = requestFederationUseCase(domain, message)) {
                is ApiResult.Success -> loadInstances()
                is ApiResult.Error -> {
                    error = result.message
                    isLoading = false
                }
                is ApiResult.NetworkError -> {
                    error = result.message
                    isLoading = false
                }
            }
        }
    }

    fun blockInstance(instanceId: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = blockInstanceUseCase(instanceId)) {
                is ApiResult.Success -> loadInstances()
                is ApiResult.Error -> {
                    error = result.message
                    isLoading = false
                }
                is ApiResult.NetworkError -> {
                    error = result.message
                    isLoading = false
                }
            }
        }
    }

    fun removeInstance(instanceId: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = removeInstanceUseCase(instanceId)) {
                is ApiResult.Success -> loadInstances()
                is ApiResult.Error -> {
                    error = result.message
                    isLoading = false
                }
                is ApiResult.NetworkError -> {
                    error = result.message
                    isLoading = false
                }
            }
        }
    }

    fun acceptShare(shareId: String) {
        viewModelScope.launch {
            error = null
            when (val result = acceptShareUseCase(shareId)) {
                is ApiResult.Success -> loadShares()
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun declineShare(shareId: String) {
        viewModelScope.launch {
            error = null
            when (val result = declineShareUseCase(shareId)) {
                is ApiResult.Success -> loadShares()
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun revokeShare(shareId: String) {
        viewModelScope.launch {
            error = null
            when (val result = revokeShareUseCase(shareId)) {
                is ApiResult.Success -> loadShares()
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun unlinkIdentity(identityId: String) {
        viewModelScope.launch {
            error = null
            when (val result = unlinkIdentityUseCase(identityId)) {
                is ApiResult.Success -> loadIdentities()
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun getInstanceDetails(domain: String) {
        viewModelScope.launch {
            error = null
            when (val result = getFederatedInstanceUseCase(domain)) {
                is ApiResult.Success -> selectedInstance = result.data
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
        }
    }

    fun linkIdentity(remoteUserId: String, remoteInstance: String, displayName: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = linkIdentityUseCase(remoteUserId, remoteInstance, displayName)) {
                is ApiResult.Success -> loadIdentities()
                is ApiResult.Error -> {
                    error = result.message
                    isLoading = false
                }
                is ApiResult.NetworkError -> {
                    error = result.message
                    isLoading = false
                }
            }
        }
    }

    fun clearSelectedInstance() {
        selectedInstance = null
    }

    fun clearError() {
        error = null
    }
}
