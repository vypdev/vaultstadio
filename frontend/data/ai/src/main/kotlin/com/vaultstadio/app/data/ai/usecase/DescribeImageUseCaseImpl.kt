/**
 * Describe Image Use Case implementation
 */

package com.vaultstadio.app.data.ai.usecase

import com.vaultstadio.app.domain.ai.AIRepository
import com.vaultstadio.app.domain.ai.usecase.DescribeImageUseCase

class DescribeImageUseCaseImpl(
    private val aiRepository: AIRepository,
) : DescribeImageUseCase {
    override suspend operator fun invoke(imageBase64: String, mimeType: String) =
        aiRepository.describeImage(imageBase64, mimeType)
}
