package tech.dokus.domain.model.common

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class DateRange(
    val start: LocalDate?,
    val end: LocalDate?
)
