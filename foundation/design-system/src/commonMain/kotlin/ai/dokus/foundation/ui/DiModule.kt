package ai.dokus.foundation.ui

import ai.dokus.foundation.ui.brandsugar.BackgroundAnimationViewModel
import org.koin.dsl.module

val uiDiModule = module {
    single<BackgroundAnimationViewModel> { BackgroundAnimationViewModel() }
}