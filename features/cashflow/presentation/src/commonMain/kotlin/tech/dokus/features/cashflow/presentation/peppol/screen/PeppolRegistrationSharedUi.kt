package tech.dokus.features.cashflow.presentation.peppol.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.constrains.limitWidthCenteredContent
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.textMuted
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun PeppolCenteredFlow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    body: (@Composable () -> Unit)? = null,
    primary: @Composable () -> Unit,
    secondary: (@Composable () -> Unit)? = null,
    details: (@Composable () -> Unit)? = null,
    footnote: String? = null,
) {
    Column(
        modifier = modifier
            .limitWidthCenteredContent()
            .verticalScroll(rememberScrollState())
            .padding(Constrains.Spacing.xxLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()

        Spacer(modifier = Modifier.height(Constrains.Spacing.xxLarge))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Constrains.Spacing.small))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (body != null) {
            Spacer(modifier = Modifier.height(Constrains.Spacing.large))
            body()
        }

        Spacer(modifier = Modifier.height(Constrains.Spacing.xxLarge))

        primary()

        if (secondary != null) {
            Spacer(modifier = Modifier.height(Constrains.Spacing.small))
            secondary()
        }

        if (footnote != null) {
            Spacer(modifier = Modifier.height(Constrains.Spacing.xLarge))
            Text(
                text = footnote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
                textAlign = TextAlign.Center,
            )
        }

        if (details != null) {
            Spacer(modifier = Modifier.height(Constrains.Spacing.large))
            details()
        }
    }
}

@Composable
internal fun PeppolCircle(
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        content?.invoke()
    }
}

@Composable
internal fun PeppolSpinner() {
    DokusLoader(size = DokusLoaderSize.Medium)
}

@Composable
internal fun PeppolCloseIcon(
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.textMuted
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 1.5.dp.toPx()
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width, 0f),
            end = Offset(0f, size.height),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
internal fun TransferEmailCard(
    companyName: String,
    peppolId: String,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val template = remember(companyName, peppolId) {
        """
        Subject: Peppol deregistration request

        Hello,

        I would like to deregister my company from your Peppol service.

        Company: $companyName
        Peppol ID: $peppolId

        Please confirm when the deregistration is complete.

        Thank you.
        """.trimIndent()
    }

    LaunchedEffect(copied) {
        if (!copied) return@LaunchedEffect
        delay(2.seconds)
        copied = false
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
            .clip(MaterialTheme.shapes.small)
    ) {
        Column {
            Text(
                text = template,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(Constrains.Spacing.medium)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.shapes.small
                    )
                    .padding(horizontal = Constrains.Spacing.medium),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(template))
                        copied = true
                    }
                ) {
                    Text(
                        text = if (copied) "Copied to clipboard" else "Copy email",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (copied) MaterialTheme.colorScheme.statusConfirmed else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
