@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.share.ShareLinkClipboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.UIKit.UIPasteboard

actual fun createShareLinkClipboard(): ShareLinkClipboard {
    val native = DirectShareLinkClipboard { text -> UIPasteboard.generalPasteboard.string = text }
    return ShareLinkClipboard { link -> withContext(Dispatchers.Main) { native.write(link) } }
}
