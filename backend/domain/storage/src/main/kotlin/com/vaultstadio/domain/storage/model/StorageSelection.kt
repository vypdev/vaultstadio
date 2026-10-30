package com.vaultstadio.domain.storage.model

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure

enum class SelectionParameter { LIMIT, OFFSET }

enum class StorageSelectionFailure(val parameter: SelectionParameter) {
    INVALID_LIMIT(SelectionParameter.LIMIT),
    INVALID_OFFSET(SelectionParameter.OFFSET),
}

/** Bounded interactive selections; protocol-specific raw queries retain their own contracts. */
class StorageSelection private constructor(val limit: Int, val offset: Int) {
    companion object {
        const val MAX_LIMIT = 1000

        fun create(limit: Int, offset: Int): Either<StorageSelectionFailure, StorageSelection> = either {
            ensure(limit in 1..MAX_LIMIT) { StorageSelectionFailure.INVALID_LIMIT }
            ensure(offset >= 0) { StorageSelectionFailure.INVALID_OFFSET }
            StorageSelection(limit, offset)
        }
    }
}
