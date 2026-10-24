/**
 * Use case for getting video metadata (duration, codec, resolution, etc.).
 */

package com.vaultstadio.app.domain.metadata.usecase

import com.vaultstadio.app.domain.metadata.model.VideoMetadata
import com.vaultstadio.app.domain.result.Result

interface GetVideoMetadataUseCase {
    suspend operator fun invoke(itemId: String): Result<VideoMetadata>
}
