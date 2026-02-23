package tech.dokus.features.auth.presentation.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.auth_protocol_http
import tech.dokus.aura.resources.auth_protocol_https
import tech.dokus.foundation.aura.constrains.Constraints

internal enum class ProtocolOption(val value: String, val labelRes: StringResource) {
    HTTP("http", Res.string.auth_protocol_http),
    HTTPS("https", Res.string.auth_protocol_https),
}

@Composable
fun ProtocolSelector(
    selectedProtocol: String,
    onProtocolSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        ProtocolOption.entries.forEach { option ->
            val isSelected = selectedProtocol == option.value
            Surface(
                onClick = { onProtocolSelected(option.value) },
                modifier = Modifier.weight(1f),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                border = androidx.compose.foundation.BorderStroke(
                    width = Constraints.Stroke.thin,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                ),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = Constraints.Elevation.none,
                shadowElevation = Constraints.Elevation.none,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = Constraints.Spacing.medium,
                            horizontal = Constraints.Spacing.large,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (option == ProtocolOption.HTTPS) {
                            Icons.Default.Lock
                        } else {
                            Icons.Default.LockOpen
                        },
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(Constraints.Spacing.small))
                    Text(
                        text = stringResource(option.labelRes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ProtocolSelectorPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        ProtocolSelector(
            selectedProtocol = "https",
            onProtocolSelected = {},
        )
    }
}
