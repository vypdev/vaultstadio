/**
 * Tag Image Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AIRepository
import org.koin.core.annotation.Factory

/**
 * Use case for generating AI tags for images.
 */
interface TagImageUseCase {
    suspend operator fun invoke(imageBase64: String, mimeType: String = "image/jpeg"): ApiResult<List<String>>
}

@Factory(binds = [TagImageUseCase::class])
class TagImageUseCaseImpl(
    private val aiRepository: AIRepository,
) : TagImageUseCase {

    override suspend operator fun invoke(imageBase64: String, mimeType: String): ApiResult<List<String>> =
        aiRepository.tagImage(imageBase64, mimeType)
}
