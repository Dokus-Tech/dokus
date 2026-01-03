package tech.dokus.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import tech.dokus.app.utils.initKoin

class DokusApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin(
            modules = appModules.diModules,
            appDeclaration = {
                androidContext(this@DokusApplication)
                androidLogger()
            }
        )
    }
}
