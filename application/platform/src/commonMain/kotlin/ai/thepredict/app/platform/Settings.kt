package ai.thepredict.app.platform

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlin.reflect.KProperty

private class SettingsDelegate(
    private val settings: Settings,
    private val key: String,
) {
    inline operator fun <reified T : Any> getValue(thisRef: Persistence, property: KProperty<*>): T? {
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
    var email: String? by SettingsDelegate(settings, "email")
    var password: String? by SettingsDelegate(settings, "password")
}

private val settings: Settings = Settings()
val persistence = Persistence(settings)