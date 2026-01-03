package tech.dokus.features.cashflow.presentation.review

private const val DpiLow = 100
private const val DpiMedium = 150
private const val DpiHigh = 200

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
    var dpi: PreviewDpi = PreviewDpi.MEDIUM
    const val DEFAULT_MAX_PAGES = 10
}
