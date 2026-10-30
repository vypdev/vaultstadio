package com.vaultstadio.app.core.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Names the existing control without replacing its edit actions, state or secure semantics. */
fun Modifier.inputSemantics(id: String, label: String): Modifier {
    require(id.isNotBlank()) { "Input identifier must not be blank" }
    require(label.isNotBlank()) { "Input label must not be blank" }
    return testTag(id).semantics { contentDescription = label }
}
