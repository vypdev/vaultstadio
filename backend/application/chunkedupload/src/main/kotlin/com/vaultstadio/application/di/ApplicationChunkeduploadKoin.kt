package com.vaultstadio.application.di

import com.vaultstadio.application.usecase.chunkedupload.CancelChunkedUploadUseCase
import com.vaultstadio.application.usecase.chunkedupload.CancelChunkedUploadUseCaseImpl
import com.vaultstadio.application.usecase.chunkedupload.CompleteChunkedUploadUseCase
import com.vaultstadio.application.usecase.chunkedupload.CompleteChunkedUploadUseCaseImpl
import com.vaultstadio.application.usecase.chunkedupload.GetChunkedUploadStatusUseCase
import com.vaultstadio.application.usecase.chunkedupload.GetChunkedUploadStatusUseCaseImpl
import com.vaultstadio.application.usecase.chunkedupload.InitChunkedUploadUseCase
import com.vaultstadio.application.usecase.chunkedupload.InitChunkedUploadUseCaseImpl
import com.vaultstadio.application.usecase.chunkedupload.UploadChunkUseCase
import com.vaultstadio.application.usecase.chunkedupload.UploadChunkUseCaseImpl
import org.koin.dsl.module

fun applicationChunkeduploadModule() = module {
    single<InitChunkedUploadUseCase> { InitChunkedUploadUseCaseImpl(get()) }
    single<GetChunkedUploadStatusUseCase> { GetChunkedUploadStatusUseCaseImpl(get()) }
    single<UploadChunkUseCase> { UploadChunkUseCaseImpl(get()) }
    single<CompleteChunkedUploadUseCase> { CompleteChunkedUploadUseCaseImpl(get(), get()) }
    single<CancelChunkedUploadUseCase> { CancelChunkedUploadUseCaseImpl(get()) }
}
