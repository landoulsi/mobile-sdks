package com.landoulsi.pushnotification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationChannel(
    val id: String,
    val name: String,
    val description: String = "",
    val importance: Importance = Importance.DEFAULT,
    val sound: String? = null,
    val vibrationPattern: List<Long> = emptyList(),
    val lockScreenVisibility: LockScreenVisibility = LockScreenVisibility.PRIVATE,
) {
    enum class Importance {
        @SerialName("none")
        NONE,

        @SerialName("min")
        MIN,

        @SerialName("low")
        LOW,

        @SerialName("default")
        DEFAULT,

        @SerialName("high")
        HIGH,

        @SerialName("max")
        MAX,
    }

    enum class LockScreenVisibility {
        @SerialName("private")
        PRIVATE,

        @SerialName("public")
        PUBLIC,

        @SerialName("secret")
        SECRET,
    }
}
