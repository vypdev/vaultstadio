package com.vaultstadio.app.data.network

/** Reject nesting before the general JSON decoder can recurse into an invalid configuration value. */
internal fun flatConfigurationShape(document: String): Boolean {
    val shape = ConfigurationShape()
    return document.all(shape::accept) && shape.complete
}

private class ConfigurationShape {
    private var quoted = false
    private var escaped = false
    private var depth = 0
    val complete: Boolean get() = !quoted && depth == 0

    fun accept(char: Char): Boolean = if (quoted) {
        stringCharacter(char)
        true
    } else {
        structureCharacter(char)
    }

    private fun stringCharacter(char: Char) {
        when {
            escaped -> escaped = false
            char == '\\' -> escaped = true
            char == '"' -> quoted = false
        }
    }

    private fun structureCharacter(char: Char): Boolean {
        if (char == '"') {
            quoted = true
            return true
        }
        return when (char) {
            '{' -> ++depth == 1
            '}' -> --depth == 0
            '[' -> false
            else -> true
        }
    }
}
