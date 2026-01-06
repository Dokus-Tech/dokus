package tech.dokus.app.navigation

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.bar_chart
import tech.dokus.aura.resources.calculator
import tech.dokus.aura.resources.cashflow
import tech.dokus.aura.resources.cashflow_title
import tech.dokus.aura.resources.chart_bar_trend_up
import tech.dokus.aura.resources.chat_title
import tech.dokus.aura.resources.file_text
import tech.dokus.aura.resources.home_dashboard
import tech.dokus.aura.resources.home_settings
import tech.dokus.aura.resources.ml
import tech.dokus.aura.resources.more_horizontal
import tech.dokus.aura.resources.nav_clients
import tech.dokus.aura.resources.nav_documents
import tech.dokus.aura.resources.nav_forecast
import tech.dokus.aura.resources.nav_more
import tech.dokus.aura.resources.nav_reports
import tech.dokus.aura.resources.nav_section_accounting
import tech.dokus.aura.resources.nav_section_company
import tech.dokus.aura.resources.nav_section_intelligence
import tech.dokus.aura.resources.nav_team
import tech.dokus.aura.resources.nav_vat
import tech.dokus.aura.resources.settings
import tech.dokus.aura.resources.trending_up
import tech.dokus.aura.resources.users
import tech.dokus.foundation.aura.model.MobileTabConfig
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.foundation.aura.model.NavSection
import tech.dokus.navigation.destinations.HomeDestination

/**
 * Navigation definition - single source of truth for all navigation items.
 *
 * Rendered differently by form factor:
 * - Desktop: Sectioned rail with expandable/collapsible sections
 * - Mobile: Bottom tabs + "More" screen with grouped navigation
 */
object NavDefinition {

    // ========================================================================
    // ROUTE STRINGS (matching @SerialName values in HomeDestination)
    // ========================================================================

    object Routes {
        const val DASHBOARD = "dashboard"
        const val DOCUMENTS = "documents"
        const val CASHFLOW = "cashflow"
        const val CONTACTS = "contacts"
        const val TEAM = "team"
        const val AI_CHAT = "ai-chat"
        const val SETTINGS = "settings"
        const val MORE = "more"
        const val UNDER_DEVELOPMENT = "home/under_development"
    }

    // ========================================================================
    // SECTION IDS
    // ========================================================================

    object SectionIds {
        const val ACCOUNTING = "accounting"
        const val COMPANY = "company"
        const val INTELLIGENCE = "intelligence"
    }

    // ========================================================================
    // NAV ITEMS
    // ========================================================================

    object Items {
        val dashboard = NavItem(
            id = "dashboard",
            titleRes = Res.string.home_dashboard,
            iconRes = Res.drawable.chart_bar_trend_up,
            route = Routes.DASHBOARD,
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

        val aiChat = NavItem(
            id = "ai_chat",
            titleRes = Res.string.chat_title,
            iconRes = Res.drawable.ml,
            route = Routes.AI_CHAT,
            comingSoon = false,
            showTopBar = false
        )

        val forecast = NavItem(
            id = "forecast",
            titleRes = Res.string.nav_forecast,
            iconRes = Res.drawable.trending_up,
            route = Routes.UNDER_DEVELOPMENT,
            comingSoon = true,
            showTopBar = true
        )

        val settings = NavItem(
            id = "settings",
            titleRes = Res.string.home_settings,
            iconRes = Res.drawable.settings,
            route = Routes.SETTINGS,
            comingSoon = false,
            showTopBar = true
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
                Items.dashboard,
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
                Items.clients,
                Items.team
            ),
            defaultExpanded = false
        ),
        NavSection(
            id = SectionIds.INTELLIGENCE,
            titleRes = Res.string.nav_section_intelligence,
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
            id = "tab_dashboard",
            titleRes = Res.string.home_dashboard,
            iconRes = Res.drawable.chart_bar_trend_up,
            route = Routes.DASHBOARD
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

    /** Map route string to HomeDestination */
    fun routeToDestination(route: String?): HomeDestination? = when (route) {
        Routes.DASHBOARD -> HomeDestination.Dashboard
        Routes.DOCUMENTS -> HomeDestination.Documents
        Routes.CASHFLOW -> HomeDestination.Cashflow
        Routes.CONTACTS -> HomeDestination.Contacts
        Routes.TEAM -> HomeDestination.Team
        Routes.AI_CHAT -> HomeDestination.AiChat
        Routes.SETTINGS -> HomeDestination.Settings
        Routes.MORE -> HomeDestination.More
        Routes.UNDER_DEVELOPMENT -> HomeDestination.UnderDevelopment
        else -> null
    }

    /** Map HomeDestination to route string */
    fun destinationToRoute(destination: HomeDestination): String = when (destination) {
        HomeDestination.Dashboard -> Routes.DASHBOARD
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
