package ai.thepredict.ui

import ai.thepredict.ui.brandsugar.BackgroundAnimationViewModel
import org.koin.dsl.module

val uiDiModule = module {
    single<BackgroundAnimationViewModel> { BackgroundAnimationViewModel() }
}