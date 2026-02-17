package com.vaultstadio.app.feature.activity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.model.Activity
import com.vaultstadio.app.domain.usecase.activity.GetRecentActivityUseCase
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

/**
 * ViewModel for displaying recent activity.
 */
@KoinViewModel
class ActivityViewModel(
    private val getRecentActivityUseCase: GetRecentActivityUseCase,
) : ViewModel() {

    var activities by mutableStateOf<List<Activity>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var selectedActivity by mutableStateOf<Activity?>(null)
        private set

    init {
        loadActivities()
    }

    fun loadActivities(limit: Int = 50) {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val result = getRecentActivityUseCase(limit)) {
                is ApiResult.Success -> activities = result.data
                is ApiResult.Error -> error = result.message
                is ApiResult.NetworkError -> error = result.message
            }
            isLoading = false
        }
    }

    fun onActivityClick(activity: Activity) {
        selectedActivity = activity
    }

    fun clearSelectedActivity() {
        selectedActivity = null
    }

    fun clearError() {
        error = null
    }
}
