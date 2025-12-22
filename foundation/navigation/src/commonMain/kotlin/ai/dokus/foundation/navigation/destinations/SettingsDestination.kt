package ai.dokus.foundation.navigation.destinations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Navigation destinations for settings screens.
 */
sealed interface SettingsDestination : NavigationDestination {

    @Serializable
    @SerialName("settings/workspace")
    data object WorkspaceSettings : SettingsDestination

    @Serializable
    @SerialName("settings/workspace/team")
    data object TeamSettings : SettingsDestination

    @Serializable
    @SerialName("settings/peppol")
    data object PeppolSettings : SettingsDestination

    sealed interface PeppolConfiguration : SettingsDestination {
        @Serializable
        @SerialName("settings/peppol/connect")
        data class Connect(val providerName: String) : PeppolConfiguration
    }

    @Serializable
    @SerialName("settings/appearance")
    data object AppearanceSettings : SettingsDestination
}
