package ai.thepredict.app.core

import org.kodein.di.DI

lateinit var di: DI

fun configureDi(vararg modules: DI.Module) {
    di = DI {
        importAll(*modules)
    }
}