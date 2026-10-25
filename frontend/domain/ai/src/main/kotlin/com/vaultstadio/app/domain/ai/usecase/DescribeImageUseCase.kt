/**
 * Use case for generating AI descriptions of images.
 */

package com.vaultstadio.app.domain.ai.usecase

import com.vaultstadio.app.domain.result.Result

interface DescribeImageUseCase {
    suspend operator fun invoke(imageBase64: String, mimeType: String = "image/jpeg"): Result<String>
}
