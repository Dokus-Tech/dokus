package tech.dokus.foundation.aura.model

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.navigation.destinations.NavigationDestination

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

/** Default shell top bar mode for a NavItem. null = no shell top bar. */
enum class ShellTopBarDefault { Search, Title }

@Immutable
data class NavItem(
    val id: String,
    val titleRes: StringResource,
    val iconRes: DrawableResource,
    /** Typed navigation destination */
    val destination: NavigationDestination,
    /** Whether this item is coming soon (disabled, reduced opacity) */
    val comingSoon: Boolean = false,
    /** Minimum subscription tier required to access this item (null = available to all tiers) */
    val requiredTier: SubscriptionTier? = null,
    /** Sort order within a section (lower = higher priority) */
    val priority: Int = 0,
    /** Non-null = appears as a mobile bottom tab at this position. null = only in desktop rail / More screen. */
    val mobileTabOrder: Int? = null,
    /** Non-null = shell renders a top bar for this item. null = screen manages its own top bar. */
    val shellTopBar: ShellTopBarDefault? = null,
)

/**
 * Configuration for mobile bottom navigation tabs.
 */
@Immutable
data class MobileTabConfig(
    val id: String,
    val titleRes: StringResource,
    val iconRes: DrawableResource,
    /** Typed navigation destination, null for "More" tab */
    val destination: NavigationDestination?,
)
