/**
 * Koin module for Collaboration (API, service, repository, use cases).
 */

package com.vaultstadio.app.data.collaboration.di

import com.vaultstadio.app.data.collaboration.api.CollaborationApi
import com.vaultstadio.app.data.collaboration.repository.CollaborationRepositoryImpl
import com.vaultstadio.app.data.collaboration.service.CollaborationService
import com.vaultstadio.app.data.collaboration.usecase.CreateDocumentCommentUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.DeleteDocumentCommentUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.GetCollaborationSessionUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.GetDocumentCommentsUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.GetDocumentStateUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.GetSessionParticipantsUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.GetUserPresenceUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.JoinCollaborationSessionUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.LeaveCollaborationSessionUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.ResolveDocumentCommentUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.SaveDocumentUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.SetOfflineUseCaseImpl
import com.vaultstadio.app.data.collaboration.usecase.UpdatePresenceUseCaseImpl
import com.vaultstadio.app.domain.collaboration.CollaborationRepository
import com.vaultstadio.app.domain.collaboration.usecase.CreateDocumentCommentUseCase
import com.vaultstadio.app.domain.collaboration.usecase.DeleteDocumentCommentUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetCollaborationSessionUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetDocumentCommentsUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetDocumentStateUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetSessionParticipantsUseCase
import com.vaultstadio.app.domain.collaboration.usecase.GetUserPresenceUseCase
import com.vaultstadio.app.domain.collaboration.usecase.JoinCollaborationSessionUseCase
import com.vaultstadio.app.domain.collaboration.usecase.LeaveCollaborationSessionUseCase
import com.vaultstadio.app.domain.collaboration.usecase.ResolveDocumentCommentUseCase
import com.vaultstadio.app.domain.collaboration.usecase.SaveDocumentUseCase
import com.vaultstadio.app.domain.collaboration.usecase.SetOfflineUseCase
import com.vaultstadio.app.domain.collaboration.usecase.UpdatePresenceUseCase
import io.ktor.client.HttpClient
import org.koin.dsl.module

val collaborationModule = module {
    single { CollaborationApi(get<HttpClient>()) }
    single { CollaborationService(get()) }
    single<CollaborationRepository> { CollaborationRepositoryImpl(get()) }

    factory<JoinCollaborationSessionUseCase> { JoinCollaborationSessionUseCaseImpl(get()) }
    factory<LeaveCollaborationSessionUseCase> { LeaveCollaborationSessionUseCaseImpl(get()) }
    factory<GetCollaborationSessionUseCase> { GetCollaborationSessionUseCaseImpl(get()) }
    factory<GetSessionParticipantsUseCase> { GetSessionParticipantsUseCaseImpl(get()) }
    factory<GetDocumentStateUseCase> { GetDocumentStateUseCaseImpl(get()) }
    factory<SaveDocumentUseCase> { SaveDocumentUseCaseImpl(get()) }
    factory<GetDocumentCommentsUseCase> { GetDocumentCommentsUseCaseImpl(get()) }
    factory<CreateDocumentCommentUseCase> { CreateDocumentCommentUseCaseImpl(get()) }
    factory<ResolveDocumentCommentUseCase> { ResolveDocumentCommentUseCaseImpl(get()) }
    factory<DeleteDocumentCommentUseCase> { DeleteDocumentCommentUseCaseImpl(get()) }
    factory<UpdatePresenceUseCase> { UpdatePresenceUseCaseImpl(get()) }
    factory<GetUserPresenceUseCase> { GetUserPresenceUseCaseImpl(get()) }
    factory<SetOfflineUseCase> { SetOfflineUseCaseImpl(get()) }
}
