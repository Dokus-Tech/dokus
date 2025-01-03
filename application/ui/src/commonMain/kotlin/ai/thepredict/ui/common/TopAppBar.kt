package ai.thepredict.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PTopAppBar(title: String, hideIfNoPop: Boolean = false) {
    val navigator = LocalNavigator.currentOrThrow
    if (!navigator.canPop && hideIfNoPop) {
        return
    }

    TopAppBar(
        title = { Text(title, textAlign = TextAlign.Center) },
        navigationIcon = {
            if (navigator.canPop) {
                IconButton(
                    onClick = { navigator.pop() }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        }
    )
}