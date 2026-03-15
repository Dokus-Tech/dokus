package tech.dokus.features.cashflow.presentation.review

private const val DpiLow = 100
private const val DpiMedium = 300
private const val DpiHigh = 600

/**
 * DPI presets for PDF preview rendering.
 */
enum class PreviewDpi(val value: Int) {
    LOW(DpiLow),
    MEDIUM(DpiMedium),
    HIGH(DpiHigh)
}

/**
 * Configuration for PDF page preview rendering.
 */
object PreviewConfig {
    val dpi: PreviewDpi = PreviewDpi.HIGH
    const val DEFAULT_MAX_PAGES = 10
}
