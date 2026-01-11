package tech.dokus.foundation.aura.screenshot

import app.cash.paparazzi.DeviceConfig
import com.android.resources.Density
import tech.dokus.foundation.aura.local.ScreenSize

/**
 * Predefined viewport sizes for screenshot testing.
 * Based on Material Design responsive breakpoints.
 */
enum class ScreenshotViewport(
    val deviceConfig: DeviceConfig,
    val displayName: String,
    val screenSize: ScreenSize,
) {
    /**
     * Compact viewport for small phones (320x568)
     * iPhone SE / Small Android devices
     */
    COMPACT(
        deviceConfig = DeviceConfig(
            screenWidth = 320,
            screenHeight = 568,
            density = Density.XHIGH,
            softButtons = false
        ),
        displayName = "compact",
        screenSize = ScreenSize.SMALL,
    ),

    /**
     * Medium viewport for standard phones/small tablets (600x960)
     * Standard Android phone / iPad Mini portrait
     */
    MEDIUM(
        deviceConfig = DeviceConfig(
            screenWidth = 600,
            screenHeight = 960,
            density = Density.XXHIGH,
            softButtons = false
        ),
        displayName = "medium",
        screenSize = ScreenSize.MEDIUM,
    ),

    /**
     * Expanded viewport for tablets/desktop (1440x900)
     * Tablet landscape / Desktop browser
     */
    EXPANDED(
        deviceConfig = DeviceConfig(
            screenWidth = 1440,
            screenHeight = 900,
            density = Density.MEDIUM,
            softButtons = false
        ),
        displayName = "expanded",
        screenSize = ScreenSize.LARGE,
    )
}
