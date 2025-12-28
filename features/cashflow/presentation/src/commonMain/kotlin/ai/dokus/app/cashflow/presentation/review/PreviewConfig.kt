package ai.dokus.app.cashflow.presentation.review

/**
 * DPI presets for PDF preview rendering.
 */
enum class PreviewDpi(val value: Int) {
    LOW(100),
    MEDIUM(150),
    HIGH(200)
}

/**
 * Configuration for PDF page preview rendering.
 */
object PreviewConfig {
    var dpi: PreviewDpi = PreviewDpi.MEDIUM
    const val DEFAULT_MAX_PAGES = 10
}
