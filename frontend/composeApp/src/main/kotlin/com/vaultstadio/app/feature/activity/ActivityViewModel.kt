package com.vaultstadio.app.feature.activity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.activity.model.Activity
import com.vaultstadio.app.domain.activity.usecase.GetRecentActivityUseCase
import kotlinx.coroutines.launch
/**
 * ViewModel for displaying recent activity.
 */
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
                is Result.Success -> activities = result.data
                is Result.Error -> error = result.message
                is Result.NetworkError -> error = result.message
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
