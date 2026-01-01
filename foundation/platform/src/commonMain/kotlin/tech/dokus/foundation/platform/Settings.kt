package tech.dokus.foundation.platform

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlin.reflect.KProperty

private class SettingsDelegate(
    private val settings: Settings,
    private val key: String,
) {
    inline operator fun <reified T : Any> getValue(
        thisRef: Persistence,
        property: KProperty<*>,
    ): T? {
        return settings.get<T>(key)
    }

    inline operator fun <reified T> setValue(
        persistence: Persistence,
        property: KProperty<*>,
        value: T?,
    ) {
        settings[key] = value
    }
}

class Persistence(private val settings: Settings) {
    var userId: String? by SettingsDelegate(settings, "user_id")
    var firstName: String? by SettingsDelegate(settings, "name")
    var lastName: String? by SettingsDelegate(settings, "last_name")
    var email: String? by SettingsDelegate(settings, "email")
    var selectedWorkspace: String? by SettingsDelegate(settings, "selected_workspace")
    var jwtToken: String? by SettingsDelegate(settings, "user_token")

    // Appearance preferences
    var themeModePreference: String? by SettingsDelegate(settings, "theme_mode")

    /**
     * Clear all user-related data.
     * Called when switching servers to ensure clean state.
     * Preserves theme preferences.
     */
    fun clearUserData() {
        userId = null
        firstName = null
        lastName = null
        email = null
        selectedWorkspace = null
        jwtToken = null
        // Note: themeModePreference is intentionally preserved
    }
}

private val settings: Settings = Settings()
val persistence = Persistence(settings)