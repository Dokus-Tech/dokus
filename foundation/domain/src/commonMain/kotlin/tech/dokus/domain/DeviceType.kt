package tech.dokus.domain

import tech.dokus.domain.database.DbEnum
import kotlinx.serialization.Serializable

@Serializable
enum class DeviceType(override val dbValue: String) : DbEnum {
    Android("ANDROID"),
    IOS("IOS"),
    Desktop("DESKTOP"),
    Web("WEB"),
    Tablet("TABLET");

    companion object {
        fun fromAgent(agent: String?): DeviceType {
            val lowerAgent = agent?.lowercase().orEmpty()
            return when {
                lowerAgent.contains("mobile") && lowerAgent.contains("android") ->
                    Android

                lowerAgent.contains("mobile") && lowerAgent.contains("iphone") ->
                    IOS

                lowerAgent.contains("tablet") || lowerAgent.contains("ipad") ->
                    Tablet

                else -> Desktop
            }
        }
    }
}

expect val DeviceType.Companion.current: DeviceType

val DeviceType.isWeb: Boolean get() = this == DeviceType.Web
val DeviceType.isDesktop: Boolean get() = this == DeviceType.Desktop
val DeviceType.isMobile: Boolean get() = this == DeviceType.Android || this == DeviceType.IOS