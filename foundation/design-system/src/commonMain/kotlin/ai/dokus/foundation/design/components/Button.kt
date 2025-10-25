package ai.dokus.foundation.design.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.arrow_left
import org.jetbrains.compose.resources.painterResource

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
    isEnabled: Boolean = true,
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

@Composable
fun PPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(8.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}


@Composable
fun PBackButton(
    modifier: Modifier,
    onBackPress: () -> Unit,
) {
    Icon(
        painter = painterResource(Res.drawable.arrow_left),
        contentDescription = "Back",
        modifier = modifier.clickable { onBackPress() }.size(24.dp)
    )
}