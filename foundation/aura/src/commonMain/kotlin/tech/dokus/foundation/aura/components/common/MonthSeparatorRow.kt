package tech.dokus.foundation.aura.components.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_unknown
import tech.dokus.aura.resources.date_month_long_april
import tech.dokus.aura.resources.date_month_long_august
import tech.dokus.aura.resources.date_month_long_december
import tech.dokus.aura.resources.date_month_long_february
import tech.dokus.aura.resources.date_month_long_january
import tech.dokus.aura.resources.date_month_long_july
import tech.dokus.aura.resources.date_month_long_june
import tech.dokus.aura.resources.date_month_long_march
import tech.dokus.aura.resources.date_month_long_may
import tech.dokus.aura.resources.date_month_long_november
import tech.dokus.aura.resources.date_month_long_october
import tech.dokus.aura.resources.date_month_long_september
import tech.dokus.foundation.aura.constrains.Constraints

@Composable
fun MonthSeparatorRow(
    year: Int,
    month: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = formatMonthYear(year, month),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Constraints.Spacing.large,
                vertical = Constraints.Spacing.small,
            ),
    )
}

@Composable
private fun formatMonthYear(year: Int, month: Int): String {
    val months = listOf(
        stringResource(Res.string.date_month_long_january),
        stringResource(Res.string.date_month_long_february),
        stringResource(Res.string.date_month_long_march),
        stringResource(Res.string.date_month_long_april),
        stringResource(Res.string.date_month_long_may),
        stringResource(Res.string.date_month_long_june),
        stringResource(Res.string.date_month_long_july),
        stringResource(Res.string.date_month_long_august),
        stringResource(Res.string.date_month_long_september),
        stringResource(Res.string.date_month_long_october),
        stringResource(Res.string.date_month_long_november),
        stringResource(Res.string.date_month_long_december),
    )
    val monthName = months.getOrElse(month - 1) {
        stringResource(Res.string.common_unknown)
    }
    return "$monthName $year"
}
