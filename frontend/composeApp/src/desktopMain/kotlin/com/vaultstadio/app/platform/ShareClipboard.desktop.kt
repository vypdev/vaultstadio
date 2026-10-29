package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.share.ClipboardWriteOutcome
import com.vaultstadio.app.domain.share.ShareLinkClipboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

actual fun createShareLinkClipboard(): ShareLinkClipboard {
    val native = DirectShareLinkClipboard { text ->
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }
    return ShareLinkClipboard { link ->
        if (GraphicsEnvironment.isHeadless()) {
            ClipboardWriteOutcome.UNAVAILABLE
        } else {
            withContext(Dispatchers.IO) { native.write(link) }
        }
    }
}
