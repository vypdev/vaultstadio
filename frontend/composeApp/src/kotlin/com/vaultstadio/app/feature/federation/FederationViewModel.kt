package com.vaultstadio.app.feature.federation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.domain.federation.model.FederatedActivity
import com.vaultstadio.app.domain.federation.model.FederatedIdentity
import com.vaultstadio.app.domain.federation.model.FederatedInstance
import com.vaultstadio.app.domain.federation.model.FederatedShare
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
import kotlinx.coroutines.launch
/**
 * ViewModel for federation management.
 */
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
                is Result.Success -> instances = result.data
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
            isLoading = false
        }
    }

    fun loadShares() {
        viewModelScope.launch {
            error = null
            when (val outgoing = getOutgoingSharesUseCase()) {
                is Result.Success -> outgoingShares = outgoing.data
                is Result.Error -> error = outgoing.message
                is Result.NetworkError -> error = outgoing.message
            }
            when (val incoming = getIncomingSharesUseCase()) {
                is Result.Success -> incomingShares = incoming.data
                is Result.Error -> if (error == null) error = incoming.message
                is Result.NetworkError -> if (error == null) error = incoming.message
            }
        }
    }

    fun loadIdentities() {
        viewModelScope.launch {
            error = null
            when (val result = getIdentitiesUseCase()) {
                is Result.Success -> identities = result.data
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun loadActivities() {
        viewModelScope.launch {
            error = null
            when (val result = getActivitiesUseCase()) {
                is Result.Success -> activities = result.data
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun requestFederation(domain: String, message: String?) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = requestFederationUseCase(domain, message)) {
                is Result.Success -> loadInstances()
                is Result.Error -> {
                    error = result.message
                    isLoading = false
                }
                is Result.NetworkError -> {
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
                is Result.Success -> loadInstances()
                is Result.Error -> {
                    error = result.message
                    isLoading = false
                }
                is Result.NetworkError -> {
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
                is Result.Success -> loadInstances()
                is Result.Error -> {
                    error = result.message
                    isLoading = false
                }
                is Result.NetworkError -> {
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
                is Result.Success -> loadShares()
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun declineShare(shareId: String) {
        viewModelScope.launch {
            error = null
            when (val result = declineShareUseCase(shareId)) {
                is Result.Success -> loadShares()
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun revokeShare(shareId: String) {
        viewModelScope.launch {
            error = null
            when (val result = revokeShareUseCase(shareId)) {
                is Result.Success -> loadShares()
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun unlinkIdentity(identityId: String) {
        viewModelScope.launch {
            error = null
            when (val result = unlinkIdentityUseCase(identityId)) {
                is Result.Success -> loadIdentities()
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun getInstanceDetails(domain: String) {
        viewModelScope.launch {
            error = null
            when (val result = getFederatedInstanceUseCase(domain)) {
                is Result.Success -> selectedInstance = result.data
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
            }
        }
    }

    fun linkIdentity(remoteUserId: String, remoteInstance: String, displayName: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = linkIdentityUseCase(remoteUserId, remoteInstance, displayName)) {
                is Result.Success -> loadIdentities()
                is Result.Error -> {
                    error = result.message
                    isLoading = false
                }
                is Result.NetworkError -> {
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
