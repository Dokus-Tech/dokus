package ai.dokus.app.cashflow.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Data class representing business health metrics.
 *
 * @property predictedPercentage The predicted health percentage (0-100)
 * @property actualPercentage The actual health percentage (0-100)
 * @property status The health status label (e.g., "Good", "Warning", "Critical")
 */
data class BusinessHealthData(
    val predictedPercentage: Int,
    val actualPercentage: Int,
    val status: HealthStatus = HealthStatus.Good
) {
    companion object {
        val empty = BusinessHealthData(
            predictedPercentage = 90,
            actualPercentage = 86,
            status = HealthStatus.Good
        )
    }
}

/**
 * Health status enum with associated display properties.
 */
enum class HealthStatus {
    Good,
    Warning,
    Critical
}

/**
 * A card component displaying business health metrics with a donut chart.
 *
 * Shows:
 * - Title "Business health"
 * - Description of what the metric represents
 * - Donut chart with predicted vs actual percentages
 * - Legend with predicted and actual values
 *
 * @param data The business health data to display
 * @param modifier Optional modifier for the card
 */
@Composable
fun BusinessHealthCard(
    data: BusinessHealthData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(24.dp)
        ) {
            // Title
            Text(
                text = "Business health",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = "The status of your current company or business is assessed based on your management of taxes, invoices, and budget.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Chart and legend row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut chart
                DonutChart(
                    actualPercentage = data.actualPercentage,
                    status = data.status,
                    modifier = Modifier.size(120.dp)
                )

                // Legend
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LegendItem(
                        color = MaterialTheme.colorScheme.primary,
                        label = "Predicted",
                        value = "${data.predictedPercentage}%"
                    )
                    LegendItem(
                        color = MaterialTheme.colorScheme.tertiary,
                        label = "Actual",
                        value = "${data.actualPercentage}%"
                    )
                }
            }
        }
    }
}

/**
 * Donut chart component showing the actual percentage with status color.
 */
@Composable
private fun DonutChart(
    actualPercentage: Int,
    status: HealthStatus,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = when (status) {
        HealthStatus.Good -> MaterialTheme.colorScheme.tertiary
        HealthStatus.Warning -> MaterialTheme.colorScheme.secondary
        HealthStatus.Critical -> MaterialTheme.colorScheme.error
    }
    val statusLabel = when (status) {
        HealthStatus.Good -> "Good"
        HealthStatus.Warning -> "Warning"
        HealthStatus.Critical -> "Critical"
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = 16.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val topLeft = Offset(
                (size.width - radius * 2) / 2,
                (size.height - radius * 2) / 2
            )
            val arcSize = Size(radius * 2, radius * 2)

            // Background arc (full circle)
            drawArc(
                color = backgroundColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            val sweepAngle = (actualPercentage / 100f) * 360f
            drawArc(
                color = progressColor,
                startAngle = -90f, // Start from top
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Center text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${actualPercentage}%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Legend item showing a color indicator and label with value.
 */
@Composable
private fun LegendItem(
    color: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .border(4.dp, color, CircleShape)
        )

        // Label and value
        Text(
            text = "$label - $value",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
