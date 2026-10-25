/**
 * Breadcrumbs row for Files screen; when in a subfolder, acts as drop target for "move to parent".
 */

package com.vaultstadio.app.feature.files

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.storage.model.Breadcrumb
import com.vaultstadio.app.ui.components.layout.Breadcrumbs

@Composable
internal fun FilesBreadcrumbsBar(
    breadcrumbs: List<Breadcrumb>,
    currentFolderId: String?,
    onNavigate: (Breadcrumb) -> Unit,
    dropTargetBounds: MutableMap<String, androidx.compose.ui.geometry.Rect>,
    dropTargetUnderPointer: String?,
    draggedItemId: String?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                if (currentFolderId != null) {
                    dropTargetBounds["parent"] = coordinates.boundsInRoot()
                } else {
                    dropTargetBounds.remove("parent")
                }
            }
            .then(
                if (dropTargetUnderPointer == "parent" && draggedItemId != null) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    Modifier
                },
            ),
    ) {
        Breadcrumbs(
            breadcrumbs = breadcrumbs,
            onNavigate = onNavigate,
        )
    }
}
