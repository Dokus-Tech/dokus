package ai.dokus.app.wrap

import ai.dokus.app.core.coreDiModule
import androidx.compose.runtime.Composable
import org.koin.compose.KoinApplication

@Composable
fun Bootstrapped(content: @Composable () -> Unit) {
    KoinApplication(
        application = {
            modules(
                coreDiModule,
            )
        }
    ) {
        content()
    }
}