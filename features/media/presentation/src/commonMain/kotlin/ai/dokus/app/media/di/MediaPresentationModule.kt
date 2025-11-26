package ai.dokus.app.media.di

import ai.dokus.app.media.viewmodel.MediaViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mediaViewModelModule = module {
    viewModel { MediaViewModel() }
}

val mediaPresentationModule = module {
    includes(mediaViewModelModule)
}
