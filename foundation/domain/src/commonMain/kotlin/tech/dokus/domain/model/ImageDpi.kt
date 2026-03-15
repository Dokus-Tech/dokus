package tech.dokus.domain.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

private const val DpiMin = 75
private const val DpiLow = 100
private const val DpiMedium = 250
private const val DpiHigh = 350
private const val DpiMax = 600

@Serializable
@JvmInline
value class Dpi private constructor(val value: Int) {
    companion object {
        val low = Dpi(DpiLow)
        val medium = Dpi(DpiMedium)
        val high = Dpi(DpiHigh)
        val max = Dpi(DpiMax)

        val default = high

        fun create(value: Int): Dpi = Dpi(value.coerceIn(DpiMin, DpiMax))
    }
}

const val DEFAULT_MAX_PAGES = 10