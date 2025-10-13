package ai.dokus.foundation.ui.common

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PTopAppBar(
    title: String,
    hideIfNoPop: Boolean = false,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {}
) {
    if (!canNavigateBack && hideIfNoPop) {
        return
    }

    CenterAlignedTopAppBar(
        title = { Text(title, textAlign = TextAlign.Center) },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(
                    onClick = onNavigateBack
                ) {
                    Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                }
            }
        }
    )
}