/**
 * Tag Image Use Case implementation
 */

package com.vaultstadio.app.data.ai.usecase

import com.vaultstadio.app.domain.ai.AIRepository
import com.vaultstadio.app.domain.ai.usecase.TagImageUseCase

class TagImageUseCaseImpl(
    private val aiRepository: AIRepository,
) : TagImageUseCase {
    override suspend operator fun invoke(imageBase64: String, mimeType: String) =
        aiRepository.tagImage(imageBase64, mimeType)
}
