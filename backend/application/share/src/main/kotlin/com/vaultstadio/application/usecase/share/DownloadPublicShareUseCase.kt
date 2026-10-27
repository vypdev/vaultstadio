package com.vaultstadio.application.usecase.share

import arrow.core.Either
import com.vaultstadio.core.share.access.PublicShareService
import com.vaultstadio.domain.share.access.AccessShareInput
import com.vaultstadio.domain.share.access.PublicShareDownload
import com.vaultstadio.domain.share.access.PublicShareFailure

interface DownloadPublicShareUseCase {
    suspend operator fun invoke(input: AccessShareInput): Either<PublicShareFailure, PublicShareDownload>
}

class DownloadPublicShareUseCaseImpl(private val service: PublicShareService) : DownloadPublicShareUseCase {
    override suspend fun invoke(input: AccessShareInput): Either<PublicShareFailure, PublicShareDownload> =
        service.download(input)
}
