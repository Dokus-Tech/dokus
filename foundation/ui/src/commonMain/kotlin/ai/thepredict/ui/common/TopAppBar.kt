package ai.thepredict.ui.common

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PTopAppBar(title: String, hideIfNoPop: Boolean = false) {
    val navigator = LocalNavigator.currentOrThrow
    if (!navigator.canPop && hideIfNoPop) {
        return
    }

    CenterAlignedTopAppBar(
        title = { Text(title, textAlign = TextAlign.Center) },
        navigationIcon = {
            if (navigator.canPop) {
                IconButton(
                    onClick = { navigator.pop() }
                ) {
                    Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                }
            }
        }
    )
}