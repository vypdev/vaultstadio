package com.vaultstadio.app.data.network

/** Reject malformed JSON string scalars before a URI encoder can silently replace them. */
internal fun validUnicodeScalars(value: String): Boolean {
    var index = 0
    while (index < value.length) {
        val char = value[index]
        when {
            char.isHighSurrogate() -> {
                if (index + 1 >= value.length || !value[index + 1].isLowSurrogate()) return false
                index++
            }
            char.isLowSurrogate() -> return false
        }
        index++
    }
    return true
}
