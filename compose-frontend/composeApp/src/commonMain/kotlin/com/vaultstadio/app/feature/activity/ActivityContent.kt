package com.vaultstadio.app.feature.activity

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaultstadio.app.ui.screens.ActivityScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * Activity feature content - delegates to ActivityScreen with ViewModel data.
 */
@Composable
fun ActivityContent(
    @Suppress("UNUSED_PARAMETER") component: ActivityComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: ActivityViewModel = koinViewModel()

    ActivityScreen(
        activities = viewModel.activities,
        selectedActivity = viewModel.selectedActivity,
        isLoading = viewModel.isLoading,
        error = viewModel.error,
        onLoadActivities = viewModel::loadActivities,
        onActivityClick = viewModel::onActivityClick,
        onClearSelectedActivity = viewModel::clearSelectedActivity,
        onClearError = viewModel::clearError,
        modifier = modifier,
    )
}
