package ai.dokus.foundation.design.components.text

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.app_name
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppNameText(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResource(Res.string.app_name),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.displaySmall
    )
}