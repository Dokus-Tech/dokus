package ai.dokus.foundation.navigation.local

/**
 * Defines the type of secondary panel and its layout characteristics.
 *
 * @property levitate Whether the panel should have a floating "levitation" animation
 * @property widthFraction The target width fraction (0.0 to 1.0) when visible on large screens
 */
enum class SecondaryPanelType(
    val levitate: Boolean,
    val widthFraction: Float
) {
    /**
     * Inline panel: Edge-to-edge, no elevation, 50% width
     * Used for side-by-side equal content areas
     */
    Inline(levitate = false, widthFraction = 0.5f),

    /**
     * Complimentary panel: Floating with elevation, 45% width
     * Used for supplementary content that enhances the primary view
     */
    Complimentary(levitate = true, widthFraction = 0.45f),

    /**
     * Info panel: Smaller sidebar for reference information, 35% width
     * Used for help panels, documentation, or quick reference
     */
    Info(levitate = false, widthFraction = 0.35f);

    /**
     * Clamps the calculated width (in dp) to min/max constraints for this panel type.
     * Only applies when the panel is showing; returns raw width when hidden.
     *
     * @param rawWidthDp The unclamped width in dp
     * @param isShowing Whether the panel is currently visible
     * @return The clamped width in dp
     */
    fun clampWidthDp(rawWidthDp: Float, isShowing: Boolean): Float {
        if (!isShowing) return rawWidthDp
        return when (this) {
            Info -> rawWidthDp.coerceIn(320f, 480f)
            Complimentary -> rawWidthDp.coerceIn(360f, 720f)
            Inline -> rawWidthDp  // No clamping for inline panels
        }
    }
}
