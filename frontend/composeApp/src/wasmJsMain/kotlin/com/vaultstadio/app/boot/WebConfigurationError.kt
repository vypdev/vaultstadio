package com.vaultstadio.app.boot

import kotlinx.browser.document
import kotlinx.browser.window

/** Native accessible failure before Compose/Koin; untrusted configuration never enters copy/HTML. */
internal fun showWebConfigurationError(
    failure: WebStartupFailure,
    containerId: String = "ComposeTarget",
    reload: () -> Unit = { window.location.reload() },
) {
    val target = document.getElementById(containerId) ?: return
    while (target.firstChild != null) target.removeChild(target.firstChild!!)
    val panel = document.createElement("main")
    panel.setAttribute("class", "loading")
    val title = document.createElement("h1")
    title.textContent = "VaultStadio cannot start"
    val message = document.createElement("p")
    message.setAttribute("role", "alert")
    message.textContent = when (failure) {
        WebStartupFailure.LOAD ->
            "The server configuration could not be loaded. Ask the operator to check it, then reload."
        WebStartupFailure.INVALID -> "The server configuration is invalid. Ask the operator to correct it, then reload."
    }
    val button = document.createElement("button")
    button.textContent = "Reload"
    button.addEventListener("click", { reload() })
    panel.appendChild(title)
    panel.appendChild(message)
    panel.appendChild(button)
    target.appendChild(panel)
}
