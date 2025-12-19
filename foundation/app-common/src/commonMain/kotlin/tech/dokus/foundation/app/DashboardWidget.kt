package tech.dokus.foundation.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@JvmInline
value class WidgetId(val value: String) {
    companion object {
        fun random() = WidgetId(Uuid.random().toString())
    }
}

/**
 * Represents a widget that can be contributed to the dashboard by a feature module.
 *
 * Each module can provide one or more dashboard widgets through their AppModule implementation.
 * Widgets are rendered in the dashboard based on their priority and span configuration.
 *
 * @property id Unique identifier for the widget (e.g., "auth.active_users")
 * @property priority Determines the display order of widgets
 * @property span Defines how much horizontal space the widget occupies
 * @property content The composable function that renders the widget UI
 */
@Immutable
data class DashboardWidget(
    val id: WidgetId = WidgetId.random(),
    val priority: DashboardWidgetPriority = DashboardWidgetPriority.Medium,
    val span: DashboardWidgetSpan = DashboardWidgetSpan.Single,
    val content: @Composable () -> Unit
)

/**
 * Priority levels for dashboard widgets.
 * Widgets are displayed in order from Critical to Low priority.
 */
enum class DashboardWidgetPriority {
    Critical,  // Always shown first, for critical system information
    High,      // Important metrics and alerts
    Medium,    // Standard widgets (default)
    Low        // Secondary information
}

/**
 * Defines the horizontal span of a widget in the dashboard grid.
 */
enum class DashboardWidgetSpan {
    Single,    // Occupies 1 column width (~300dp minimum)
    Double,    // Occupies 2 columns width (~600dp minimum)
    Full       // Occupies full row width
}

/**
 * Extension property to collect all dashboard widgets from a collection of AppModules.
 * Widgets are sorted by priority (Critical -> High -> Medium -> Low).
 */
val Collection<AppModule>.dashboardWidgets: List<DashboardWidget>
    get() = flatMap { it.dashboardWidgets }
        .sortedBy { it.priority }
