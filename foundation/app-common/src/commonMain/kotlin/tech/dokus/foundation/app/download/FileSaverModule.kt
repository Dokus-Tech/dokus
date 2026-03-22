package tech.dokus.foundation.app.download

import org.koin.dsl.module

val fileSaverModule = module {
    single { FileSaver() }
}
