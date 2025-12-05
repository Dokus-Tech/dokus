package ai.dokus.app.media.di

import org.koin.dsl.module

val mediaViewModelModule = module {}

val mediaPresentationModule = module {
    includes(mediaViewModelModule)
}
