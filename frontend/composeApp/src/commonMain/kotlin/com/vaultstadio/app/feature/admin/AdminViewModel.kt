package com.vaultstadio.app.feature.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.model.AdminUser
import com.vaultstadio.app.domain.model.UserRole
import com.vaultstadio.app.domain.model.UserStatus
import com.vaultstadio.app.domain.usecase.admin.GetAdminUsersUseCase
import com.vaultstadio.app.domain.usecase.admin.UpdateUserQuotaUseCase
import com.vaultstadio.app.domain.usecase.admin.UpdateUserRoleUseCase
import com.vaultstadio.app.domain.usecase.admin.UpdateUserStatusUseCase
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

/**
 * ViewModel for admin user management.
 */
@KoinViewModel
class AdminViewModel(
    private val getAdminUsersUseCase: GetAdminUsersUseCase,
    private val updateUserQuotaUseCase: UpdateUserQuotaUseCase,
    private val updateUserRoleUseCase: UpdateUserRoleUseCase,
    private val updateUserStatusUseCase: UpdateUserStatusUseCase,
) : ViewModel() {

    var users by mutableStateOf<List<AdminUser>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = getAdminUsersUseCase()) {
                is ApiResult.Success -> users = result.data.items
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
            isLoading = false
        }
    }

    fun updateUserQuota(userId: String, quotaBytes: Long?) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = updateUserQuotaUseCase(userId, quotaBytes)) {
                is ApiResult.Success -> loadUsers()
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

    fun updateUserRole(userId: String, role: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            val userRole = try {
                UserRole.valueOf(role.uppercase())
            } catch (e: IllegalArgumentException) {
                error = "Invalid role: $role"
                isLoading = false
                return@launch
            }
            when (val result = updateUserRoleUseCase(userId, userRole)) {
                is ApiResult.Success -> loadUsers()
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

    fun updateUserStatus(userId: String, status: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            val userStatus = try {
                UserStatus.valueOf(status.uppercase())
            } catch (e: IllegalArgumentException) {
                error = "Invalid status: $status"
                isLoading = false
                return@launch
            }
            when (val result = updateUserStatusUseCase(userId, userStatus)) {
                is ApiResult.Success -> loadUsers()
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

    fun clearError() {
        error = null
    }
}
