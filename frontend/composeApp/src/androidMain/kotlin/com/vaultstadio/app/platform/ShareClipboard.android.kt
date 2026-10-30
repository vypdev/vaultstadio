package com.vaultstadio.app.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.vaultstadio.app.domain.share.ClipboardWriteOutcome
import com.vaultstadio.app.domain.share.ShareLinkClipboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

actual fun createShareLinkClipboard(): ShareLinkClipboard {
    // The Android application registers this context before resolving its platform binding.
    val context: Context = GlobalContext.get().get()
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val native = DirectShareLinkClipboard { text ->
        checkNotNull(manager).setPrimaryClip(ClipData.newPlainText("VaultStadio link", text))
    }
    return ShareLinkClipboard { link ->
        if (manager == null) {
            ClipboardWriteOutcome.UNAVAILABLE
        } else {
            withContext(Dispatchers.Main) { native.write(link) }
        }
    }
}
