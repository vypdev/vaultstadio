/**
 * Upload action triggers for the Files screen (picker is run in LaunchedEffect in FilesContent).
 */

package com.vaultstadio.app.feature.files

/** User chose an upload option from the menu; FilesContent's LaunchedEffect runs the picker. */
internal sealed class UploadAction {
    data object Files : UploadAction()
    data object Folder : UploadAction()
    data object LargeFiles : UploadAction()
}
