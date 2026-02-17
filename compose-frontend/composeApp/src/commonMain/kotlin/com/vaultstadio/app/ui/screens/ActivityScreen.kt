/**
 * VaultStadio Activity Screen
 *
 * Screen for viewing user activity log.
 */

package com.vaultstadio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.Activity
import com.vaultstadio.app.ui.screens.activity.ActivityCard
import com.vaultstadio.app.ui.screens.activity.ActivityDetailDialog
import com.vaultstadio.app.ui.screens.activity.ActivityErrorDialog
import com.vaultstadio.app.ui.screens.activity.EmptyActivityState

/**
 * Activity type filter.
 */
enum class ActivityFilter {
    ALL,
    FILES,
    SHARING,
    AUTH,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    activities: List<Activity>,
    selectedActivity: Activity?,
    isLoading: Boolean,
    error: String?,
    onLoadActivities: () -> Unit,
    onActivityClick: (Activity) -> Unit,
    onClearSelectedActivity: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFilter by remember { mutableStateOf(ActivityFilter.ALL) }

    LaunchedEffect(Unit) {
        onLoadActivities()
    }

    val filteredActivities = remember(activities, selectedFilter) {
        when (selectedFilter) {
            ActivityFilter.ALL -> activities
            ActivityFilter.FILES -> activities.filter {
                it.type in listOf(
                    "upload", "download", "create", "delete", "move",
                    "rename", "copy", "restore", "trash", "star",
                )
            }
            ActivityFilter.SHARING -> activities.filter {
                it.type in listOf("share", "unshare", "share_access")
            }
            ActivityFilter.AUTH -> activities.filter {
                it.type in listOf("login", "logout", "register", "password_change")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Activity") })
        },
    ) { padding ->
        Column(
            modifier = modifier.fillMaxSize().padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActivityFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                when (filter) {
                                    ActivityFilter.ALL -> "All"
                                    ActivityFilter.FILES -> "Files"
                                    ActivityFilter.SHARING -> "Sharing"
                                    ActivityFilter.AUTH -> "Auth"
                                },
                            )
                        },
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    filteredActivities.isEmpty() -> {
                        EmptyActivityState(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(filteredActivities) { activity ->
                                ActivityCard(
                                    activity = activity,
                                    onClick = { onActivityClick(activity) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedActivity?.let { activity ->
        ActivityDetailDialog(activity = activity, onDismiss = onClearSelectedActivity)
    }
    error?.let { message ->
        ActivityErrorDialog(message = message, onDismiss = onClearError)
    }
}
