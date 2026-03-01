package tech.dokus.foundation.aura.style

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class DokusSpacing(
    val xxSmall: Dp = 2.dp,
    val xSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val xLarge: Dp = 24.dp,
    val xxLarge: Dp = 32.dp,
    val xxxLarge: Dp = 48.dp,
)

@Immutable
data class DokusSizing(
    val largeScreenWidth: Dp = 980.dp,
    val largeScreenDefaultWidth: Dp = 1280.dp,
    val largeScreenHeight: Dp = 840.dp,
    val centeredContentMaxWidth: Dp = 420.dp,
    val shellPadding: Dp = 10.dp,
    val shellGap: Dp = 10.dp,
    val shellSidebarWidth: Dp = 220.dp,
    val shellContentPaddingV: Dp = 24.dp,
    val shellContentPaddingH: Dp = 28.dp,
    val documentQueueWidth: Dp = 220.dp,
    val documentInspectorWidth: Dp = 272.dp,
    val documentPreviewMaxWidth: Dp = 480.dp,
    val statusDotSize: Dp = 5.dp,
    val tableRowMinHeight: Dp = 42.dp,
    val tableHeaderPaddingH: Dp = 22.dp,
    val iconXSmall: Dp = 16.dp,
    val iconSmall: Dp = 18.dp,
    val iconSmallMedium: Dp = 20.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val iconXLarge: Dp = 48.dp,
    val iconXXLarge: Dp = 64.dp,
    val buttonLoadingIcon: Dp = 20.dp,
    val buttonHeight: Dp = 34.dp,
    val inputHeight: Dp = 56.dp,
    val navigationBarHeight: Dp = 60.dp,
    val shimmerLineHeight: Dp = 14.dp,
    val elevationNone: Dp = 0.dp,
    val elevationModal: Dp = 1.dp,
    val avatarExtraSmall: Dp = 24.dp,
    val avatarSmall: Dp = 32.dp,
    val avatarMedium: Dp = 64.dp,
    val avatarTile: Dp = 72.dp,
    val avatarLarge: Dp = 128.dp,
    val avatarExtraLarge: Dp = 256.dp,
    val strokeThin: Dp = 1.dp,
    val strokeDashWidth: Dp = 6.dp,
    val strokeCropGuide: Dp = 3.dp,
    val dialogMaxWidth: Dp = 400.dp,
    val dialogCropAreaMax: Dp = 320.dp,
    val cropGuideCornerLength: Dp = 40.dp,
    val navigationFabSize: Dp = 46.dp,
    val navigationIndicatorWidth: Dp = 24.dp,
    val navigationIndicatorHeight: Dp = 2.dp,
    val searchFieldMinWidth: Dp = 200.dp,
    val searchFieldMaxWidth: Dp = 360.dp,
    val operatorFormMaxWidth: Dp = 820.dp,
)

@Immutable
data class DokusRadii(
    val badge: Dp = 4.dp,
    val input: Dp = 6.dp,
    val button: Dp = 7.dp,
    val card: Dp = 10.dp,
    val window: Dp = 16.dp,
)

@Immutable
data class DokusEffects(
    val ambientOrbAmber: Color,
    val ambientOrbNeutral: Color,
    val ambientParticleGold: Color,
    val ambientParticleNeutral: Color,
    val ambientSweepAlpha: Float,
    val ambientSweepColor: Color,
    val loaderPrimary: Color,
    val loaderSecondary: Color,
    val loaderAccent: Color,
    val loaderBaseAlpha: Float,
    val railTrackLine: Color,
    val railActiveBackground: Color,
    val railBadgeBackground: Color,
    val uploadParticleOrange: Color,
    val uploadParticleAmber: Color,
    val uploadParticleRed: Color,
    val uploadParticleDarkOrange: Color,
    val uploadParticleLightOrange: Color,
    val uploadLensingRing: Color,
    val uploadEventHorizonInner: Color,
    val uploadEventHorizonMiddle: Color,
    val uploadEventHorizonOuter: Color,
    val uploadVoidCenter: Color,
    val uploadVoidEdge: Color,
    val uploadVoidOuter: Color,
    val uploadCoreHighlight: Color,
    val uploadLensingArc: Color,
    val uploadTrailInner: Color,
    val uploadTrailOuter: Color,
    val uploadHeatGlowInner: Color,
    val uploadHeatGlowOuter: Color,
    val warpSilver: Color,
    val warpLightSilver: Color,
    val warpGainsboro: Color,
    val warpLightGray: Color,
    val warpGray: Color,
    val warpMediumSilver: Color,
    val warpNeutralGray: Color,
    val warpPaleGray: Color,
    val warpFlashColor: Color,
    val warpFadeColor: Color,
)

internal val DefaultDokusSpacing = DokusSpacing()
internal val DefaultDokusSizing = DokusSizing()
internal val DefaultDokusRadii = DokusRadii()

internal fun createDokusEffects(colorScheme: ColorScheme): DokusEffects = if (colorScheme.isDark) {
    DokusEffects(
        ambientOrbAmber = Color(0xFFD4A017),
        ambientOrbNeutral = Color(0xFF3D3832),
        ambientParticleGold = Color(0xFFB8860B),
        ambientParticleNeutral = Color(0xFF9C8C7C),
        ambientSweepAlpha = 0.03f,
        ambientSweepColor = Color(0xFFD4A017),
        loaderPrimary = Color(0xFFfbbf24),
        loaderSecondary = Color(0xFF71717a),
        loaderAccent = Color(0xFFf59e0b),
        loaderBaseAlpha = 0.85f,
        railTrackLine = Color.White.copy(alpha = 0.06f),
        railActiveBackground = Color.White.copy(alpha = 0.08f),
        railBadgeBackground = Color.White.copy(alpha = 0.08f),
        uploadParticleOrange = Color(0xFFFF6B35),
        uploadParticleAmber = Color(0xFFFFAA00),
        uploadParticleRed = Color(0xFFFF4444),
        uploadParticleDarkOrange = Color(0xFFCC3300),
        uploadParticleLightOrange = Color(0xFFFFDD88),
        uploadLensingRing = Color(0xFF4466AA),
        uploadEventHorizonInner = Color(0xFFFF4400),
        uploadEventHorizonMiddle = Color(0xFFFF6600),
        uploadEventHorizonOuter = Color(0xFFFFAA00),
        uploadVoidCenter = Color.Black,
        uploadVoidEdge = Color(0xFF110808),
        uploadVoidOuter = Color(0xFF1A0505),
        uploadCoreHighlight = Color.White,
        uploadLensingArc = Color(0xFF6688CC),
        uploadTrailInner = Color(0xFFFF6600),
        uploadTrailOuter = Color(0xFFFFAA00),
        uploadHeatGlowInner = Color(0xFFFF4400),
        uploadHeatGlowOuter = Color(0xFFFF6600),
        warpSilver = Color(0xFFC0C0C0),
        warpLightSilver = Color(0xFFE8E8E8),
        warpGainsboro = Color(0xFFDCDCDC),
        warpLightGray = Color(0xFFD3D3D3),
        warpGray = Color(0xFFBDBDBD),
        warpMediumSilver = Color(0xFFB8B8B8),
        warpNeutralGray = Color(0xFFCCCCCC),
        warpPaleGray = Color(0xFFE0E0E0),
        warpFlashColor = Color.White,
        warpFadeColor = Color.Black,
    )
} else {
    DokusEffects(
        ambientOrbAmber = Color(0xFFB8860B),
        ambientOrbNeutral = Color(0xFF9C958C),
        ambientParticleGold = Color(0xFFB8860B),
        ambientParticleNeutral = Color(0xFF9C8C7C),
        ambientSweepAlpha = 0.06f,
        ambientSweepColor = Color.White,
        loaderPrimary = Color(0xFF78350f),
        loaderSecondary = Color(0xFFa1a1aa),
        loaderAccent = Color(0xFF451a03),
        loaderBaseAlpha = 0.70f,
        railTrackLine = Color.Black.copy(alpha = 0.06f),
        railActiveBackground = Color.White.copy(alpha = 0.55f),
        railBadgeBackground = Color.Black.copy(alpha = 0.03f),
        uploadParticleOrange = Color(0xFFFF6B35),
        uploadParticleAmber = Color(0xFFFFAA00),
        uploadParticleRed = Color(0xFFFF4444),
        uploadParticleDarkOrange = Color(0xFFCC3300),
        uploadParticleLightOrange = Color(0xFFFFDD88),
        uploadLensingRing = Color(0xFF4466AA),
        uploadEventHorizonInner = Color(0xFFFF4400),
        uploadEventHorizonMiddle = Color(0xFFFF6600),
        uploadEventHorizonOuter = Color(0xFFFFAA00),
        uploadVoidCenter = Color.Black,
        uploadVoidEdge = Color(0xFF110808),
        uploadVoidOuter = Color(0xFF1A0505),
        uploadCoreHighlight = Color.White,
        uploadLensingArc = Color(0xFF6688CC),
        uploadTrailInner = Color(0xFFFF6600),
        uploadTrailOuter = Color(0xFFFFAA00),
        uploadHeatGlowInner = Color(0xFFFF4400),
        uploadHeatGlowOuter = Color(0xFFFF6600),
        warpSilver = Color(0xFFC0C0C0),
        warpLightSilver = Color(0xFFE8E8E8),
        warpGainsboro = Color(0xFFDCDCDC),
        warpLightGray = Color(0xFFD3D3D3),
        warpGray = Color(0xFFBDBDBD),
        warpMediumSilver = Color(0xFFB8B8B8),
        warpNeutralGray = Color(0xFFCCCCCC),
        warpPaleGray = Color(0xFFE0E0E0),
        warpFlashColor = Color.White,
        warpFadeColor = Color.Black,
    )
}

internal val LocalDokusSpacing = staticCompositionLocalOf { DefaultDokusSpacing }
internal val LocalDokusSizing = staticCompositionLocalOf { DefaultDokusSizing }
internal val LocalDokusRadii = staticCompositionLocalOf { DefaultDokusRadii }
internal val LocalDokusEffects = staticCompositionLocalOf<DokusEffects> {
    error("DokusEffects not provided. Wrap content in Themed.")
}

val MaterialTheme.dokusSpacing: DokusSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalDokusSpacing.current

val MaterialTheme.dokusSizing: DokusSizing
    @Composable
    @ReadOnlyComposable
    get() = LocalDokusSizing.current

val MaterialTheme.dokusRadii: DokusRadii
    @Composable
    @ReadOnlyComposable
    get() = LocalDokusRadii.current

val MaterialTheme.dokusEffects: DokusEffects
    @Composable
    @ReadOnlyComposable
    get() = LocalDokusEffects.current
