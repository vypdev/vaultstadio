/**
 * Use case for getting image metadata (EXIF, dimensions, etc.).
 */

package com.vaultstadio.app.domain.metadata.usecase

import com.vaultstadio.app.domain.metadata.model.ImageMetadata
import com.vaultstadio.app.domain.result.Result

interface GetImageMetadataUseCase {
    suspend operator fun invoke(itemId: String): Result<ImageMetadata>
}
