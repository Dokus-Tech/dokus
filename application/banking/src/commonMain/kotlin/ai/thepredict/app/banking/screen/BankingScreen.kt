package ai.thepredict.app.banking.screen

import ai.thepredict.app.navigation.AppNavigator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun BankingScreen(navigator: AppNavigator) {
    Scaffold {
        Box(modifier = Modifier.padding(it)) {
            Text("Banking", modifier = Modifier.align(Alignment.Center))
        }
    }
}