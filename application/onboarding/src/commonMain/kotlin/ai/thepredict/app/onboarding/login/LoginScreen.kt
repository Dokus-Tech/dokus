package ai.thepredict.app.onboarding.login

import ai.thepredict.app.core.di
import ai.thepredict.domain.Contact
import ai.thepredict.repository.api.UnifiedApi
import ai.thepredict.ui.Title
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.streamScoped
import org.kodein.di.instance

internal class LoginScreen : Screen {

//    private val api: UnifiedApi by di.instance()

    @Composable
    override fun Content() {
        val api: UnifiedApi by di.instance()
        val scope = rememberCoroutineScope()

        var clients by rememberSaveable { mutableStateOf<List<Contact>>(emptyList()) }

        LaunchedEffect("login") {
            scope.launch {
                streamScoped {
                    clients = api.getAll().toList()
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Box(
                Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)
            ) {
                Title("LoginScreen")
            }

            clients.forEach {
                Title(it.name)
            }
        }
    }
}