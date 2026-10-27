package com.vaultstadio.core.domain.service

/** Supported registration shape; identity verification is a separate capability. */
internal object UserInputValidation {
    fun email(value: String): Boolean {
        if (value.any { it.isWhitespace() }) return false
        val parts = value.split('@')
        if (parts.size != 2 || parts[0].isBlank()) return false
        val labels = parts[1].split('.')
        return labels.size >= 2 && labels.all { it.isNotBlank() }
    }

    fun password(value: String): Boolean = value.length >= 8 &&
        value.any { it.isLetter() } &&
        value.any { it.isDigit() }
}
