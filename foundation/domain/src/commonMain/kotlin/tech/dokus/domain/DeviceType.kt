package tech.dokus.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.database.DbEnum

@Serializable
enum class DeviceType(override val dbValue: String) : DbEnum {
    @SerialName("ANDROID")
    Android("ANDROID"),

    @SerialName("IOS")
    Ios("IOS"),

    @SerialName("DESKTOP")
    Desktop("DESKTOP"),

    @SerialName("WEB")
    Web("WEB"),

    @SerialName("TABLET")
    Tablet("TABLET");

    companion object {
        fun fromAgent(agent: String?): DeviceType {
            val lowerAgent = agent?.lowercase().orEmpty()
            return when {
                lowerAgent.contains("mobile") && lowerAgent.contains("android") ->
                    Android

                lowerAgent.contains("mobile") && lowerAgent.contains("iphone") ->
                    Ios

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
val DeviceType.isMobile: Boolean get() = this == DeviceType.Android || this == DeviceType.Ios
