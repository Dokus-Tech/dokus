@file:Suppress("TopLevelPropertyNaming")

package tech.dokus.foundation.aura.components.background

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.style.dokusEffects
import tech.dokus.foundation.aura.style.dokusSizing
import kotlin.math.sqrt

// Animation timing
private const val RevealDurationMs = 800
private const val SkeletonFadeDelayMs = 400
private const val SkeletonFadeDurationMs = 400

// Skeleton shimmer line widths (fraction of parent)
private val SkeletonLineWidths = floatArrayOf(0.6f, 0.45f, 0.55f, 0.35f, 0.5f)
private val SkeletonContentWidths = floatArrayOf(1f, 0.85f, 0.92f, 0.78f, 0.88f)

// Skeleton dimensions
private val SkeletonNavLineHeight = 8.dp
private val SkeletonNavLineSpacing = 12.dp
private val SkeletonNavLineMarginStart = 6.dp
private val SkeletonHeaderTitleHeight = 10.dp
private val SkeletonHeaderTitleWidth = 90.dp
private val SkeletonHeaderSubtitleHeight = 6.dp
private val SkeletonHeaderSubtitleWidth = 140.dp
private val SkeletonHeaderBadgeHeight = 6.dp
private val SkeletonHeaderBadgeWidth = 60.dp
private val SkeletonContentRowHeight = 12.dp
private val SkeletonLogoSize = 22.dp
private val SkeletonLogoRadius = 5.dp

/**
 * Radial reveal transition effect for workspace selection.
 *
 * A circle expands outward from [origin] (or screen center), revealing a surface overlay
 * with an app shell skeleton that fades up.
 *
 * @param isActive Whether the reveal animation is currently active
 * @param origin The position to expand from (defaults to screen center)
 * @param onAnimationComplete Callback when the animation completes
 */
@Composable
fun RadialRevealEffect(
    isActive: Boolean,
    origin: Offset? = null,
    onAnimationComplete: () -> Unit = {},
) {
    if (LocalInspectionMode.current) return
    if (!isActive) return

    val effects = MaterialTheme.dokusEffects
    val sizing = MaterialTheme.dokusSizing
    val density = LocalDensity.current

    // Track container size for max radius calculation
    var containerSize by remember { mutableStateOf(Size.Zero) }

    // Reveal radius animation (0 → 1 normalized)
    val revealProgress = remember { Animatable(0f) }

    // Skeleton fade-up animation
    val skeletonAlpha = remember { Animatable(0f) }
    val skeletonOffsetY = remember { Animatable(8f) }

    LaunchedEffect(Unit) {
        // Start reveal expansion
        revealProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = RevealDurationMs,
                easing = FastOutSlowInEasing,
            ),
        )
        onAnimationComplete()
    }

    LaunchedEffect(Unit) {
        // Delayed skeleton fade-up
        kotlinx.coroutines.delay(SkeletonFadeDelayMs.toLong())
        // Run both alpha and offset in parallel via coroutineScope
        kotlinx.coroutines.coroutineScope {
            kotlinx.coroutines.launch {
                skeletonAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = SkeletonFadeDurationMs,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
            kotlinx.coroutines.launch {
                skeletonOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = SkeletonFadeDurationMs,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
        }
    }

    // Calculate the origin point and max radius
    val center = origin ?: Offset(containerSize.width / 2f, containerSize.height / 2f)
    val maxRadius = if (containerSize != Size.Zero) {
        val dx = maxOf(center.x, containerSize.width - center.x)
        val dy = maxOf(center.y, containerSize.height - center.y)
        sqrt(dx * dx + dy * dy) + with(density) { 50.dp.toPx() }
    } else {
        0f
    }

    val currentRadius = maxRadius * revealProgress.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                containerSize = Size(
                    coordinates.size.width.toFloat(),
                    coordinates.size.height.toFloat(),
                )
            }
            .clip(
                CircleClipShape(
                    center = center,
                    radius = currentRadius,
                )
            )
            .background(effects.revealSurface),
    ) {
        // App shell skeleton
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(sizing.shellPadding)
                .graphicsLayer {
                    alpha = skeletonAlpha.value
                    translationY = with(density) { skeletonOffsetY.value.dp.toPx() }
                },
            horizontalArrangement = Arrangement.spacedBy(sizing.shellGap),
        ) {
            // Sidebar skeleton
            SidebarSkeleton(
                modifier = Modifier
                    .width(sizing.shellSidebarWidth)
                    .fillMaxHeight(),
            )

            // Content skeleton
            ContentSkeleton(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun SidebarSkeleton(modifier: Modifier = Modifier) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.06f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.02f))
            .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        // Logo placeholder
        Row {
            Box(
                modifier = Modifier
                    .width(SkeletonLogoSize)
                    .height(SkeletonLogoSize)
                    .clip(RoundedCornerShape(SkeletonLogoRadius))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(13.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
            )
        }

        Spacer(Modifier.height(20.dp))

        // Nav shimmer lines
        SkeletonLineWidths.forEachIndexed { index, widthFraction ->
            Box(
                modifier = Modifier
                    .padding(start = SkeletonNavLineMarginStart)
                    .fillMaxWidth(widthFraction)
                    .height(SkeletonNavLineHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (index == 1) {
                            MaterialTheme.dokusEffects.ambientOrbAmber.copy(alpha = 0.08f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                        }
                    ),
            )
            Spacer(Modifier.height(SkeletonNavLineSpacing))
        }
    }
}

@Composable
private fun ContentSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.015f))
            .padding(24.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .width(SkeletonHeaderTitleWidth)
                        .height(SkeletonHeaderTitleHeight)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(SkeletonHeaderSubtitleWidth)
                        .height(SkeletonHeaderSubtitleHeight)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)),
                )
            }
            Box(
                modifier = Modifier
                    .width(SkeletonHeaderBadgeWidth)
                    .height(SkeletonHeaderBadgeHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)),
            )
        }

        Spacer(Modifier.height(24.dp))

        // Content rows
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.01f))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SkeletonContentWidths.forEachIndexed { index, widthFraction ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(widthFraction)
                        .height(SkeletonContentRowHeight)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.015f + index * 0.003f
                            )
                        ),
                )
            }
        }
    }
}

/**
 * A [Shape] that clips to a circle with the given [center] and [radius].
 */
private class CircleClipShape(
    private val center: Offset,
    private val radius: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(
        Path().apply {
            addOval(
                Rect(
                    left = center.x - radius,
                    top = center.y - radius,
                    right = center.x + radius,
                    bottom = center.y + radius,
                )
            )
        }
    )
}
