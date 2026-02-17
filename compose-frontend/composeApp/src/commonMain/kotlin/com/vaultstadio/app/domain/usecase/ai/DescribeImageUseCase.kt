/**
 * Describe Image Use Case
 */

package com.vaultstadio.app.domain.usecase.ai

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AIRepository
import org.koin.core.annotation.Factory

/**
 * Use case for generating AI descriptions of images.
 */
interface DescribeImageUseCase {
    suspend operator fun invoke(imageBase64: String, mimeType: String = "image/jpeg"): ApiResult<String>
}

@Factory(binds = [DescribeImageUseCase::class])
class DescribeImageUseCaseImpl(
    private val aiRepository: AIRepository,
) : DescribeImageUseCase {

    override suspend operator fun invoke(imageBase64: String, mimeType: String): ApiResult<String> =
        aiRepository.describeImage(imageBase64, mimeType)
}
