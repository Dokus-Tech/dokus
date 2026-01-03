package tech.dokus.foundation.aura.components.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.copyright

@Composable
fun CopyRightText(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResource(Res.string.copyright),
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleSmall
    )
}
