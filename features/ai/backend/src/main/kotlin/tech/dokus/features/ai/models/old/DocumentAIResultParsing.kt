package tech.dokus.features.ai.models.old

import kotlinx.datetime.LocalDate
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.ExpenseCategory

internal fun String.parseLocalDate(): LocalDate? =
    runCatching { LocalDate.parse(this) }.getOrNull()

internal fun String.parseMoney(): Money? =
    runCatching { Money.parse(this) }.getOrNull()

internal fun String.parseVatRate(): VatRate? =
    runCatching { VatRate.parse(this) }.getOrNull()

internal fun String.parseExpenseCategory(): ExpenseCategory? =
    runCatching { ExpenseCategory.valueOf(uppercase().replace(" ", "_")) }.getOrNull()
