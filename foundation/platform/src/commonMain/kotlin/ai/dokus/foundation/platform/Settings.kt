package ai.dokus.foundation.platform

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

class Persistence(settings: Settings) {
    var userId: String? by SettingsDelegate(settings, "user_id")
    var firstName: String? by SettingsDelegate(settings, "name")
    var lastName: String? by SettingsDelegate(settings, "last_name")
    var email: String? by SettingsDelegate(settings, "email")
    var selectedWorkspace: String? by SettingsDelegate(settings, "selected_workspace")
    var jwtToken: String? by SettingsDelegate(settings, "user_token")
}

private val settings: Settings = Settings()
val persistence = Persistence(settings)