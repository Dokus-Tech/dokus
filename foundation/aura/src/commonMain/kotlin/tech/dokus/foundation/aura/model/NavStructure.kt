package tech.dokus.foundation.aura.model

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import tech.dokus.domain.enums.SubscriptionTier

/**
 * Navigation structure - single source of truth for all navigation items.
 * Rendered differently by form factor:
 * - Desktop: Sectioned rail with expandable/collapsible sections
 * - Mobile: Bottom tabs + "More" screen with grouped navigation
 */
@Immutable
data class NavSection(
    val id: String,
    val titleRes: StringResource,
    /** Icon for section header (displayed on parent groups only) */
    val iconRes: DrawableResource,
    val items: List<NavItem>,
    /** Default expanded state for desktop rail */
    val defaultExpanded: Boolean = true,
)

@Immutable
data class NavItem(
    val id: String,
    val titleRes: StringResource,
    val iconRes: DrawableResource,
    /** Route path for navigation */
    val route: String,
    /** Whether this item is coming soon (disabled, reduced opacity) */
    val comingSoon: Boolean = false,
    /** Whether to show top bar for this destination */
    val showTopBar: Boolean = false,
    /** Minimum subscription tier required to access this item (null = available to all tiers) */
    val requiredTier: SubscriptionTier? = null,
)

/**
 * Configuration for mobile bottom navigation tabs.
 */
@Immutable
data class MobileTabConfig(
    val id: String,
    val titleRes: StringResource,
    val iconRes: DrawableResource,
    /** Route path for navigation, null for "More" tab */
    val route: String?,
)
