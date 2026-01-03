package tech.dokus.foundation.aura.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_back
import tech.dokus.aura.resources.arrow_left
import tech.dokus.foundation.aura.constrains.Constrains

enum class PButtonVariant {
    Default,
    CallToAction,
    Outline
}

enum class PIconPosition { Leading, Trailing }

object PButtonDefaults {
    val variant: PButtonVariant = PButtonVariant.Default
    val icon: ImageVector? = null
    val contentDescription: String? = null
    val iconPosition: PIconPosition = PIconPosition.Leading
}

@Composable
fun PButton(
    text: String,
    variant: PButtonVariant = PButtonDefaults.variant,
    icon: ImageVector? = PButtonDefaults.icon,
    contentDescription: String? = PButtonDefaults.contentDescription,
    iconPosition: PIconPosition = PButtonDefaults.iconPosition,
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    when (variant) {
        PButtonVariant.Default -> PButtonDefault(
            text = text,
            icon = icon,
            contentDescription = contentDescription,
            iconPosition = iconPosition,
            isLoading = isLoading,
            isEnabled = isEnabled,
            modifier = modifier,
            onClick = onClick
        )

        PButtonVariant.CallToAction -> {}
        PButtonVariant.Outline -> {
            OutlinedButton(
                onClick = onClick,
                enabled = isEnabled && !isLoading,
                modifier = modifier.height(Constrains.Height.button),
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(
                    horizontal = Constrains.Spacing.large,
                    vertical = Constrains.Spacing.small
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Constrains.IconSize.buttonLoading),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    if (icon != null && iconPosition == PIconPosition.Leading) {
                        Icon(
                            imageVector = icon,
                            contentDescription = contentDescription,
                            modifier = Modifier.size(Constrains.IconSize.medium).padding(end = Constrains.Spacing.small)
                        )
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (icon != null && iconPosition == PIconPosition.Trailing) {
                        Icon(
                            imageVector = icon,
                            contentDescription = contentDescription,
                            modifier = Modifier.size(
                                Constrains.IconSize.medium
                            ).padding(start = Constrains.Spacing.small)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PButtonDefault(
    text: String,
    icon: ImageVector? = PButtonDefaults.icon,
    contentDescription: String? = PButtonDefaults.contentDescription,
    iconPosition: PIconPosition = PButtonDefaults.iconPosition,
    isLoading: Boolean = false,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        enabled = isEnabled && !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Constrains.IconSize.buttonLoading),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            if (icon != null && iconPosition == PIconPosition.Leading) {
                Icon(
                    icon,
                    modifier = Modifier.padding(end = Constrains.Spacing.small),
                    contentDescription = contentDescription
                )
            }
            Text(text, modifier = Modifier.padding(Constrains.Spacing.xSmall))
            if (icon != null && iconPosition == PIconPosition.Trailing) {
                Icon(
                    icon,
                    modifier = Modifier.padding(start = Constrains.Spacing.small),
                    contentDescription = contentDescription
                )
            }
        }
    }
}

@Composable
fun PPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(Constrains.Height.button),
        shape = MaterialTheme.shapes.small,
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Constrains.IconSize.buttonLoading),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun POutlinedButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(Constrains.Height.button),
        shape = MaterialTheme.shapes.small,
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Constrains.IconSize.buttonLoading),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PBackButton(
    modifier: Modifier = Modifier,
    onBackPress: () -> Unit,
) {
    IconButton(
        onClick = onBackPress,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(Res.drawable.arrow_left),
            contentDescription = stringResource(Res.string.action_back),
            modifier = Modifier.size(Constrains.IconSize.medium)
        )
    }
}
