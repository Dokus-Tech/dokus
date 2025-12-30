package tech.dokus.contacts.components.create

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.contacts_step_confirm
import ai.dokus.app.resources.generated.contacts_step_details
import ai.dokus.app.resources.generated.contacts_step_search
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Step in the create contact flow.
 */
enum class CreateContactStep(val labelRes: StringResource) {
    Search(Res.string.contacts_step_search),
    Confirm(Res.string.contacts_step_confirm),
    Details(Res.string.contacts_step_details)
}

/**
 * Horizontal step indicator showing progress through the create contact flow.
 *
 * Visual design:
 * - Active step: Primary color, filled circle
 * - Completed step: Primary color, checkmark inside
 * - Future step: Gray outline circle
 * - Lines connect the circles, colored based on completion
 */
@Composable
fun StepIndicator(
    currentStep: CreateContactStep,
    modifier: Modifier = Modifier,
) {
    val steps = CreateContactStep.entries
    val currentIndex = steps.indexOf(currentStep)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circles with connecting lines
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, step ->
                // Step circle
                StepCircle(
                    isCompleted = index < currentIndex,
                    isActive = index == currentIndex,
                )

                // Connecting line (except after last step)
                if (index < steps.lastIndex) {
                    StepLine(
                        isCompleted = index < currentIndex,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            steps.forEachIndexed { index, step ->
                Text(
                    text = stringResource(step.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        index < currentIndex -> MaterialTheme.colorScheme.primary
                        index == currentIndex -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StepCircle(
    isCompleted: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(32.dp)) {
            val radius = size.minDimension / 2
            val strokeWidth = 2.dp.toPx()

            when {
                isCompleted -> {
                    // Filled circle with primary color
                    drawCircle(
                        color = primaryColor,
                        radius = radius
                    )
                }
                isActive -> {
                    // Filled circle with primary color
                    drawCircle(
                        color = primaryColor,
                        radius = radius
                    )
                }
                else -> {
                    // Outline circle
                    drawCircle(
                        color = surfaceColor,
                        radius = radius
                    )
                    drawCircle(
                        color = outlineColor,
                        radius = radius - strokeWidth / 2,
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }

        // Checkmark for completed steps
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StepLine(
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
) {
    val completedColor = MaterialTheme.colorScheme.primary
    val incompleteColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier
            .height(2.dp)
            .fillMaxWidth()
    ) {
        drawLine(
            color = if (isCompleted) completedColor else incompleteColor,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            cap = StrokeCap.Round
        )
    }
}
