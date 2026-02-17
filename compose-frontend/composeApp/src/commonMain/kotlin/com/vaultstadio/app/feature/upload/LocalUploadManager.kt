/**
 * CompositionLocal for the global upload manager so any screen can add files to the queue.
 */

package com.vaultstadio.app.feature.upload

import androidx.compose.runtime.compositionLocalOf

val LocalUploadManager = compositionLocalOf<UploadManager?> { null }
