package ai.thepredict.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class PButtonVariant {
    Default,
    CallToAction,
    Outline
}

object PButtonDefaults {
    val variant: PButtonVariant = PButtonVariant.Default
    val icon: ImageVector? = null
    val contentDescription: String? = null
}

@Composable
fun PButton(
    text: String,
    variant: PButtonVariant = PButtonDefaults.variant,
    icon: ImageVector? = PButtonDefaults.icon,
    contentDescription: String? = PButtonDefaults.contentDescription,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    when (variant) {
        PButtonVariant.Default -> PButtonDefault(
            text = text,
            icon = icon,
            contentDescription = contentDescription,
            modifier = modifier,
            onClick = onClick
        )

        PButtonVariant.CallToAction -> {}
        PButtonVariant.Outline -> {
            OutlinedButton(
                onClick = onClick
            ) {
                if (icon != null) {
                    Icon(
                        icon,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        contentDescription = contentDescription
                    )
                }
                Text(text, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PButtonDefault(
    text: String,
    icon: ImageVector? = PButtonDefaults.icon,
    contentDescription: String? = PButtonDefaults.contentDescription,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(modifier = modifier, onClick = onClick) {
        if (icon != null) {
            Icon(
                icon,
                modifier = Modifier.padding(horizontal = 8.dp),
                contentDescription = contentDescription
            )
        }
        Text(text, modifier = Modifier.padding(4.dp))
    }
}