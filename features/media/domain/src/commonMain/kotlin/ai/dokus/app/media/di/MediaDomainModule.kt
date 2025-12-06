package ai.dokus.app.media.di

import ai.dokus.app.media.domain.usecases.AttachMediaUseCaseImpl
import ai.dokus.app.media.domain.usecases.GetMediaUseCaseImpl
import ai.dokus.app.media.domain.usecases.ListMediaUseCaseImpl
import ai.dokus.app.media.domain.usecases.UpdateMediaProcessingUseCaseImpl
import ai.dokus.app.media.domain.usecases.UploadMediaUseCaseImpl
import ai.dokus.foundation.domain.usecases.AttachMediaUseCase
import ai.dokus.foundation.domain.usecases.GetMediaUseCase
import ai.dokus.foundation.domain.usecases.ListMediaUseCase
import ai.dokus.foundation.domain.usecases.UpdateMediaProcessingUseCase
import ai.dokus.foundation.domain.usecases.UploadMediaUseCase
import org.koin.dsl.module

/**
 * Media domain module providing use case implementations.
 *
 * Use cases are bound to interfaces from foundation/domain so any
 * module can inject them without depending on media module directly.
 */
val mediaDomainModule = module {
    factory<UploadMediaUseCase> { UploadMediaUseCaseImpl(get()) }
    factory<ListMediaUseCase> { ListMediaUseCaseImpl(get()) }
    factory<GetMediaUseCase> { GetMediaUseCaseImpl(get()) }
    factory<AttachMediaUseCase> { AttachMediaUseCaseImpl(get()) }
    factory<UpdateMediaProcessingUseCase> { UpdateMediaProcessingUseCaseImpl(get()) }
}
