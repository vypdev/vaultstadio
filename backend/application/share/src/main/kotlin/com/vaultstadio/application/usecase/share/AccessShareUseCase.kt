package com.vaultstadio.application.usecase.share

import arrow.core.Either
import com.vaultstadio.core.share.access.PublicShareService
import com.vaultstadio.domain.share.access.AccessShareInput
import com.vaultstadio.domain.share.access.PublicShareFailure
import com.vaultstadio.domain.share.access.PublicShareInfo

interface AccessShareUseCase {
    suspend operator fun invoke(input: AccessShareInput): Either<PublicShareFailure, PublicShareInfo>
}

class AccessShareUseCaseImpl(private val service: PublicShareService) : AccessShareUseCase {
    override suspend fun invoke(input: AccessShareInput): Either<PublicShareFailure, PublicShareInfo> =
        service.information(input)
}
