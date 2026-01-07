package tech.dokus.app.navigation

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.bar_chart
import tech.dokus.aura.resources.calculator
import tech.dokus.aura.resources.cashflow
import tech.dokus.aura.resources.cashflow_title
import tech.dokus.aura.resources.chart_bar_trend_up
import tech.dokus.aura.resources.chat_title
import tech.dokus.aura.resources.file_text
import tech.dokus.aura.resources.home_today
import tech.dokus.aura.resources.inbox
import tech.dokus.aura.resources.ml
import tech.dokus.aura.resources.more_horizontal
import tech.dokus.aura.resources.nav_clients
import tech.dokus.aura.resources.nav_documents
import tech.dokus.aura.resources.nav_forecast
import tech.dokus.aura.resources.nav_more
import tech.dokus.aura.resources.nav_reports
import tech.dokus.aura.resources.nav_section_accounting
import tech.dokus.aura.resources.nav_section_company
import tech.dokus.aura.resources.nav_tomorrow
import tech.dokus.aura.resources.nav_team
import tech.dokus.aura.resources.nav_vat
import tech.dokus.aura.resources.settings_peppol
import tech.dokus.aura.resources.settings_workspace_details
import tech.dokus.aura.resources.trending_up
import tech.dokus.aura.resources.user
import tech.dokus.aura.resources.users
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.foundation.aura.model.MobileTabConfig
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.foundation.aura.model.NavSection
import tech.dokus.navigation.destinations.NavigationDestination
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.SettingsDestination

/**
 * Navigation definition - single source of truth for all navigation items.
 *
 * Rendered differently by form factor:
 * - Desktop: Sectioned rail with expandable/collapsible sections
 * - Mobile: Bottom tabs + "More" screen with grouped navigation
 */
object NavDefinition {

    // ========================================================================
    // ROUTE STRINGS (matching @SerialName values in HomeDestination / SettingsDestination)
    // ========================================================================

    object Routes {
        const val TODAY = "today"
        const val TOMORROW = "tomorrow"
        const val DOCUMENTS = "documents"
        const val CASHFLOW = "cashflow"
        const val CONTACTS = "contacts"
        const val TEAM = "team"
        const val AI_CHAT = "ai-chat"
        const val SETTINGS = "settings"
        const val WORKSPACE_SETTINGS = "settings/workspace"
        const val PEPPOL_SETTINGS = "settings/peppol"
        const val MORE = "more"
        const val UNDER_DEVELOPMENT = "home/under_development"
    }

    // ========================================================================
    // SECTION IDS
    // ========================================================================

    object SectionIds {
        const val ACCOUNTING = "accounting"
        const val COMPANY = "company"
        const val TOMORROW = "tomorrow"
    }

    // ========================================================================
    // NAV ITEMS
    // ========================================================================

    object Items {
        val today = NavItem(
            id = "today",
            titleRes = Res.string.home_today,
            iconRes = Res.drawable.chart_bar_trend_up,
            route = Routes.TODAY,
            comingSoon = false,
            showTopBar = false
        )

        val documents = NavItem(
            id = "documents",
            titleRes = Res.string.nav_documents,
            iconRes = Res.drawable.file_text,
            route = Routes.DOCUMENTS,
            comingSoon = false,
            showTopBar = true
        )

        val cashflow = NavItem(
            id = "cashflow",
            titleRes = Res.string.cashflow_title,
            iconRes = Res.drawable.cashflow,
            route = Routes.CASHFLOW,
            comingSoon = false,
            showTopBar = false
        )

        val vat = NavItem(
            id = "vat",
            titleRes = Res.string.nav_vat,
            iconRes = Res.drawable.calculator,
            route = Routes.UNDER_DEVELOPMENT,
            comingSoon = true,
            showTopBar = true
        )

        val reports = NavItem(
            id = "reports",
            titleRes = Res.string.nav_reports,
            iconRes = Res.drawable.bar_chart,
            route = Routes.UNDER_DEVELOPMENT,
            comingSoon = true,
            showTopBar = true
        )

        val companyDetails = NavItem(
            id = "company_details",
            titleRes = Res.string.settings_workspace_details,
            iconRes = Res.drawable.user,
            route = Routes.WORKSPACE_SETTINGS,
            comingSoon = false,
            showTopBar = true
        )

        val clients = NavItem(
            id = "clients",
            titleRes = Res.string.nav_clients,
            iconRes = Res.drawable.users,
            route = Routes.CONTACTS,
            comingSoon = false,
            showTopBar = true
        )

        val team = NavItem(
            id = "team",
            titleRes = Res.string.nav_team,
            iconRes = Res.drawable.users,
            route = Routes.TEAM,
            comingSoon = false,
            showTopBar = true
        )

        val peppol = NavItem(
            id = "peppol",
            titleRes = Res.string.settings_peppol,
            iconRes = Res.drawable.inbox,
            route = Routes.PEPPOL_SETTINGS,
            comingSoon = false,
            showTopBar = true
        )

        val aiChat = NavItem(
            id = "ai_chat",
            titleRes = Res.string.chat_title,
            iconRes = Res.drawable.ml,
            route = Routes.AI_CHAT,
            comingSoon = false,
            showTopBar = false,
            requiredTier = SubscriptionTier.One
        )

        val forecast = NavItem(
            id = "forecast",
            titleRes = Res.string.nav_forecast,
            iconRes = Res.drawable.trending_up,
            route = Routes.UNDER_DEVELOPMENT,
            comingSoon = true,
            showTopBar = true,
            requiredTier = SubscriptionTier.One
        )

    }

    // ========================================================================
    // SECTIONS (for desktop rail)
    // ========================================================================

    val sections: List<NavSection> = listOf(
        NavSection(
            id = SectionIds.ACCOUNTING,
            titleRes = Res.string.nav_section_accounting,
            iconRes = Res.drawable.chart_bar_trend_up,
            items = listOf(
                Items.today,
                Items.documents,
                Items.cashflow,
                Items.vat,
                Items.reports
            ),
            defaultExpanded = true
        ),
        NavSection(
            id = SectionIds.COMPANY,
            titleRes = Res.string.nav_section_company,
            iconRes = Res.drawable.users,
            items = listOf(
                Items.companyDetails,
                Items.clients,
                Items.team,
                Items.peppol
            ),
            defaultExpanded = false
        ),
        NavSection(
            id = SectionIds.TOMORROW,
            titleRes = Res.string.nav_tomorrow,
            iconRes = Res.drawable.ml,
            items = listOf(
                Items.aiChat,
                Items.forecast
            ),
            defaultExpanded = false
        )
    )

    // ========================================================================
    // MOBILE TABS (bottom navigation)
    // ========================================================================

    val mobileTabs: List<MobileTabConfig> = listOf(
        MobileTabConfig(
            id = "tab_today",
            titleRes = Res.string.home_today,
            iconRes = Res.drawable.chart_bar_trend_up,
            route = Routes.TODAY
        ),
        MobileTabConfig(
            id = "tab_documents",
            titleRes = Res.string.nav_documents,
            iconRes = Res.drawable.file_text,
            route = Routes.DOCUMENTS
        ),
        MobileTabConfig(
            id = "tab_cashflow",
            titleRes = Res.string.cashflow_title,
            iconRes = Res.drawable.cashflow,
            route = Routes.CASHFLOW
        ),
        MobileTabConfig(
            id = "tab_more",
            titleRes = Res.string.nav_more,
            iconRes = Res.drawable.more_horizontal,
            route = Routes.MORE
        )
    )

    // ========================================================================
    // HELPERS
    // ========================================================================

    /** Get all nav items flattened */
    val allItems: List<NavItem> = sections.flatMap { it.items }

    /** Find nav item by route */
    fun findByRoute(route: String?): NavItem? = allItems.find { it.route == route }

    /** Find section containing a route */
    fun findSectionByRoute(route: String?): NavSection? {
        if (route == null) return null
        return sections.find { section ->
            section.items.any { it.route == route }
        }
    }

    /** Map route string to navigation destination */
    fun routeToDestination(route: String?): NavigationDestination? = when (route) {
        Routes.TODAY -> HomeDestination.Today
        Routes.TOMORROW -> HomeDestination.Tomorrow
        Routes.DOCUMENTS -> HomeDestination.Documents
        Routes.CASHFLOW -> HomeDestination.Cashflow
        Routes.CONTACTS -> HomeDestination.Contacts
        Routes.TEAM -> HomeDestination.Team
        Routes.AI_CHAT -> HomeDestination.AiChat
        Routes.SETTINGS -> HomeDestination.Settings
        Routes.WORKSPACE_SETTINGS -> SettingsDestination.WorkspaceSettings
        Routes.PEPPOL_SETTINGS -> SettingsDestination.PeppolSettings
        Routes.MORE -> HomeDestination.More
        Routes.UNDER_DEVELOPMENT -> HomeDestination.UnderDevelopment
        else -> null
    }

    /** Map HomeDestination to route string */
    fun destinationToRoute(destination: HomeDestination): String = when (destination) {
        HomeDestination.Today -> Routes.TODAY
        HomeDestination.Tomorrow -> Routes.TOMORROW
        HomeDestination.Documents -> Routes.DOCUMENTS
        HomeDestination.Cashflow -> Routes.CASHFLOW
        HomeDestination.Contacts -> Routes.CONTACTS
        HomeDestination.Team -> Routes.TEAM
        HomeDestination.AiChat -> Routes.AI_CHAT
        HomeDestination.Settings -> Routes.SETTINGS
        HomeDestination.More -> Routes.MORE
        HomeDestination.UnderDevelopment -> Routes.UNDER_DEVELOPMENT
    }
}
