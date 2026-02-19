/**
 * Tag Image Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.AIRepository
/**
 * Use case for generating AI tags for images.
 */
interface TagImageUseCase {
    suspend operator fun invoke(imageBase64: String, mimeType: String = "image/jpeg"): Result<List<String>>
}

class TagImageUseCaseImpl(
    private val aiRepository: AIRepository,
) : TagImageUseCase {

    override suspend operator fun invoke(imageBase64: String, mimeType: String): Result<List<String>> =
        aiRepository.tagImage(imageBase64, mimeType)
}
