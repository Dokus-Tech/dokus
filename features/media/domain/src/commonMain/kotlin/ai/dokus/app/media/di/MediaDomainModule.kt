package ai.dokus.app.media.di

import ai.dokus.app.media.domain.usecases.AttachMediaUseCase
import ai.dokus.app.media.domain.usecases.GetMediaUseCase
import ai.dokus.app.media.domain.usecases.ListMediaUseCase
import ai.dokus.app.media.domain.usecases.ListPendingMediaUseCase
import ai.dokus.app.media.domain.usecases.UpdateMediaProcessingUseCase
import ai.dokus.app.media.domain.usecases.UploadMediaUseCase
import org.koin.dsl.module

val mediaDomainModule = module {
    factory { UploadMediaUseCase(get()) }
    factory { ListMediaUseCase(get()) }
    factory { ListPendingMediaUseCase(get()) }
    factory { GetMediaUseCase(get()) }
    factory { AttachMediaUseCase(get()) }
    factory { UpdateMediaProcessingUseCase(get()) }
}
