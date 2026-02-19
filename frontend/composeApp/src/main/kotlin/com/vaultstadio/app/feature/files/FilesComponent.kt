package com.vaultstadio.app.feature.files

import com.arkivanov.decompose.ComponentContext
import com.vaultstadio.app.feature.main.MainComponent

/**
 * Component for file management screens.
 *
 * [pathSegments] is the optional path from the URL (e.g. /files/folder/subfolder -> ["folder", "subfolder"])
 * used to open that folder on load; empty when navigating from the sidebar.
 */
interface FilesComponent {
    val mode: MainComponent.FilesMode

    /** Path segments from the URL (e.g. ["my_folder", "other_folder"] for /files/my_folder/other_folder). */
    val pathSegments: List<String>
}

/**
 * Default implementation of FilesComponent.
 */
class DefaultFilesComponent(
    componentContext: ComponentContext,
    override val mode: MainComponent.FilesMode,
    override val pathSegments: List<String> = emptyList(),
) : FilesComponent, ComponentContext by componentContext
