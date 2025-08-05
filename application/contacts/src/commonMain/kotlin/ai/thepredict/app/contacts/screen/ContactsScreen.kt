package ai.thepredict.app.contacts.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen

internal class ContactsScreen : Screen {
    @Composable
    override fun Content() {
        Scaffold {
            Box(modifier = Modifier.padding(it)) {
                Text("Contacts", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}