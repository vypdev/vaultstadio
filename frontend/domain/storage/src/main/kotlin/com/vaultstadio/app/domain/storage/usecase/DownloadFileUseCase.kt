/**
 * Download File Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result

/**
 * Use case for downloading a file.
 */
interface DownloadFileUseCase {
    suspend operator fun invoke(itemId: String): Result<ByteArray>
    fun getDownloadUrl(itemId: String): String
}
