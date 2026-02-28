package tech.dokus.foundation.aura.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val CardPadding = 16.dp
private val IndicatorSize = 24.dp
private val DotSize = 10.dp

@Composable
fun PSelectableCommandCard(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    reason: String? = null,
    enabled: Boolean = true
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val borderColor = if (selected) selectedColor else MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(IndicatorSize)
                        .border(
                            width = 2.dp,
                            color = if (selected) selectedColor else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(DotSize)
                                .background(selectedColor, CircleShape)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!badge.isNullOrBlank()) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = selectedColor,
                                modifier = Modifier
                                    .background(
                                        color = selectedColor.copy(alpha = 0.10f),
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.textMuted
                        )
                    }
                }
            }

            reason?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    modifier = Modifier.padding(start = IndicatorSize + Constraints.Spacing.small)
                )
            }
        }
    }
}

@Preview(name = "Command Card Selected")
@Composable
private fun PSelectableCommandCardSelectedPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PSelectableCommandCard(
            title = "Send via PEPPOL",
            subtitle = "E-invoice to client's accounting system",
            selected = true,
            onClick = {},
            badge = "RECOMMENDED"
        )
    }
}

@Preview(name = "Command Card Unselected")
@Composable
private fun PSelectableCommandCardUnselectedPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PSelectableCommandCard(
            title = "Export PDF",
            subtitle = "Download invoice PDF",
            selected = false,
            onClick = {}
        )
    }
}

@Preview(name = "Command Card Disabled")
@Composable
private fun PSelectableCommandCardDisabledPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PSelectableCommandCard(
            title = "Send via PEPPOL",
            subtitle = "Client has no PEPPOL ID",
            selected = true,
            onClick = {},
            reason = "Select a client to enable PEPPOL.",
            enabled = false
        )
    }
}
