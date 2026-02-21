package tech.dokus.app.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.nav_accountant
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.style.amberSoft
import tech.dokus.foundation.aura.style.borderAmber
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val MaxContentWidth = 560.dp
private val ContentPaddingH = 16.dp

/**
 * Readiness item for the current period card.
 */
data class ReadinessItem(
    val label: String,
    val count: Int,
    val ready: Boolean,
    val subtitle: String? = null,
)

/**
 * Previous period summary.
 */
data class PreviousPeriod(
    val name: String,
    val status: String,
)

/**
 * Accountant screen — export & compliance.
 * Pure UI composable with parameters (no MVI container).
 */
@Composable
fun AccountantScreen(
    periodTitle: String = "Q1 2026",
    periodRange: String = "January \u2013 March 2026",
    readinessItems: List<ReadinessItem> = sampleReadinessItems(),
    previousPeriods: List<PreviousPeriod> = samplePreviousPeriods(),
    accountantConnected: Boolean = false,
    onPrepareExport: () -> Unit = {},
    onConnectAccountant: () -> Unit = {},
) {
    val isLargeScreen = LocalScreenSize.current.isLarge

    Scaffold(
        topBar = {
            if (!isLargeScreen) PTopAppBar(Res.string.nav_accountant)
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = MaxContentWidth)
                    .padding(horizontal = ContentPaddingH)
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Current period hero card
                CurrentPeriodCard(
                    periodTitle = periodTitle,
                    periodRange = periodRange,
                    readinessItems = readinessItems,
                    onPrepareExport = onPrepareExport,
                )

                // Previous periods section
                if (previousPeriods.isNotEmpty()) {
                    Column {
                        SectionTitle(text = "Previous periods")
                        Spacer(Modifier.height(8.dp))
                        DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                previousPeriods.forEachIndexed { index, period ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = period.name,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = period.status,
                                            fontSize = 10.5.sp,
                                            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                                            color = MaterialTheme.colorScheme.textFaint,
                                        )
                                    }
                                    if (index < previousPeriods.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 18.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Accountant connection section
                Column {
                    SectionTitle(text = "Your accountant")
                    Spacer(Modifier.height(8.dp))
                    DokusCardSurface(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onConnectAccountant,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Placeholder avatar
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "+",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.textMuted,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Connect your accountant",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Give read-only access to exports",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.textMuted,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// =============================================================================
// Current Period Card

// =============================================================================

@Composable
private fun CurrentPeriodCard(
    periodTitle: String,
    periodRange: String,
    readinessItems: List<ReadinessItem>,
    onPrepareExport: () -> Unit,
) {
    DokusCardSurface(
        modifier = Modifier.fillMaxWidth(),
        accent = true,
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header: icon + period info
            Row(verticalAlignment = Alignment.Top) {
                // Download icon square
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.borderAmber,
                            shape = RoundedCornerShape(9.dp)
                        )
                        .then(
                            Modifier.padding(0.dp) // Background via card accent
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "\u2193",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = periodTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = periodRange,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.textMuted,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Readiness checklist
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                readinessItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            StatusDot(
                                type = if (item.ready) StatusDotType.Confirmed else StatusDotType.Empty,
                                size = 5.dp,
                            )
                            Column {
                                Text(
                                    text = item.label,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                if (item.subtitle != null) {
                                    Text(
                                        text = item.subtitle,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.textMuted,
                                    )
                                }
                            }
                        }
                        Text(
                            text = item.count.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                            color = if (item.count > 0) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.textFaint
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(18.dp))

            // CTA button
            PPrimaryButton(
                text = "Prepare export",
                onClick = onPrepareExport,
                modifier = Modifier.fillMaxWidth(),
            )

            // Footnote
            Text(
                text = "Generates a structured package for your accountant",
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = TextAlign.Center,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.textMuted,
            )
        }
    }
}

// =============================================================================
// Sample data
// =============================================================================

private fun sampleReadinessItems() = listOf(
    ReadinessItem(label = "Invoices received", count = 10, ready = true),
    ReadinessItem(label = "Credit notes", count = 0, ready = true),
    ReadinessItem(label = "Payment records", count = 0, ready = false, subtitle = "Connect bank to enable"),
)

private fun samplePreviousPeriods() = listOf(
    PreviousPeriod(name = "Q4 2025", status = "No data"),
    PreviousPeriod(name = "Q3 2025", status = "No data"),
)

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun AccountantScreenPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        AccountantScreen()
    }
}
