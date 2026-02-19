/**
 * Describe Image Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.AIRepository
/**
 * Use case for generating AI descriptions of images.
 */
interface DescribeImageUseCase {
    suspend operator fun invoke(imageBase64: String, mimeType: String = "image/jpeg"): Result<String>
}

class DescribeImageUseCaseImpl(
    private val aiRepository: AIRepository,
) : DescribeImageUseCase {

    override suspend operator fun invoke(imageBase64: String, mimeType: String): Result<String> =
        aiRepository.describeImage(imageBase64, mimeType)
}
