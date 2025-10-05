package ai.thepredict.ui

import ai.thepredict.ui.brandsugar.BackgroundAnimationViewModel
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton

val uiDiModule = DI.Module("ui") {
    bind<BackgroundAnimationViewModel>() with singleton { BackgroundAnimationViewModel() }
}