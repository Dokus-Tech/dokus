package ai.dokus.app

import ai.dokus.app.utils.initKoin
import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

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
