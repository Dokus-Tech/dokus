package tech.dokus.features.cashflow.presentation.common.utils

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_unknown
import tech.dokus.aura.resources.date_format_short
import tech.dokus.aura.resources.date_month_short_apr
import tech.dokus.aura.resources.date_month_short_aug
import tech.dokus.aura.resources.date_month_short_dec
import tech.dokus.aura.resources.date_month_short_feb
import tech.dokus.aura.resources.date_month_short_jan
import tech.dokus.aura.resources.date_month_short_jul
import tech.dokus.aura.resources.date_month_short_jun
import tech.dokus.aura.resources.date_month_short_mar
import tech.dokus.aura.resources.date_month_short_may
import tech.dokus.aura.resources.date_month_short_nov
import tech.dokus.aura.resources.date_month_short_oct
import tech.dokus.aura.resources.date_month_short_sep

@Composable
internal fun formatShortDate(date: LocalDate): String {
    val months = listOf(
        stringResource(Res.string.date_month_short_jan),
        stringResource(Res.string.date_month_short_feb),
        stringResource(Res.string.date_month_short_mar),
        stringResource(Res.string.date_month_short_apr),
        stringResource(Res.string.date_month_short_may),
        stringResource(Res.string.date_month_short_jun),
        stringResource(Res.string.date_month_short_jul),
        stringResource(Res.string.date_month_short_aug),
        stringResource(Res.string.date_month_short_sep),
        stringResource(Res.string.date_month_short_oct),
        stringResource(Res.string.date_month_short_nov),
        stringResource(Res.string.date_month_short_dec)
    )
    val monthName = months.getOrElse(date.month.ordinal) {
        stringResource(Res.string.common_unknown)
    }
    return stringResource(Res.string.date_format_short, monthName, date.day, date.year)
}
