/**
 * Use case for generating AI tags for images.
 */

package com.vaultstadio.app.domain.ai.usecase

import com.vaultstadio.app.domain.result.Result

interface TagImageUseCase {
    suspend operator fun invoke(imageBase64: String, mimeType: String = "image/jpeg"): Result<List<String>>
}
