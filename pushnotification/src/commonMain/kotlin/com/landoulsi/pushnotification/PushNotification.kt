package com.landoulsi.pushnotification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PushNotification(
    val id: String,
    val title: String,
    val body: String,
    val payload: Map<String, String> = emptyMap(),
    val deepLink: String? = null,
    val priority: Priority = Priority.DEFAULT,
    val sentAt: Long? = null,
    val createdAt: Long? = null,
) {
    enum class Priority {
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

    /**
     * Flattens this notification into a flat string map suitable for FCM/APNs
     * data payloads. Payload entries are namespaced with a `data.` prefix so
     * they can never collide with the reserved top-level fields.
     */
    fun toMap(): Map<String, String> = buildMap {
        put(KEY_ID, id)
        put(KEY_TITLE, title)
        put(KEY_BODY, body)
        deepLink?.let { put(KEY_DEEP_LINK, it) }
        put(KEY_PRIORITY, priority.name.lowercase())
        sentAt?.let { put(KEY_SENT_AT, it.toString()) }
        createdAt?.let { put(KEY_CREATED_AT, it.toString()) }
        payload.forEach { (key, value) -> put("$DATA_PREFIX$key", value) }
    }

    companion object {
        private const val DATA_PREFIX = "data."
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        private const val KEY_DEEP_LINK = "deepLink"
        private const val KEY_PRIORITY = "priority"
        private const val KEY_SENT_AT = "sentAt"
        private const val KEY_CREATED_AT = "createdAt"

        /**
         * Reconstructs a [PushNotification] from a flat string map. Payload
         * entries may be found under the `data.` prefix produced by [toMap],
         * or as top-level keys (as received from FCM remote messages).
         *
         * @throws IllegalArgumentException if a required field is missing.
         */
        fun fromMap(map: Map<String, String>): PushNotification {
            // First, try to extract payload under the data. prefix (toMap format)
            val dataPayload = map
                .filterKeys { it.startsWith(DATA_PREFIX) }
                .mapKeys { it.key.removePrefix(DATA_PREFIX) }

            val knownTopLevel = setOf(KEY_ID, KEY_TITLE, KEY_BODY, KEY_DEEP_LINK, KEY_PRIORITY, KEY_SENT_AT, KEY_CREATED_AT)
            val fallbackPayload = map.filterKeys { !knownTopLevel.contains(it) && !it.startsWith(DATA_PREFIX) }

            // Merge: data. prefix takes priority, fallback fills any gaps
            val mergedPayload = fallbackPayload + dataPayload

            return PushNotification(
                id = map[KEY_ID] ?: throw IllegalArgumentException("Missing 'id'"),
                title = map[KEY_TITLE] ?: throw IllegalArgumentException("Missing 'title'"),
                body = map[KEY_BODY] ?: throw IllegalArgumentException("Missing 'body'"),
                payload = mergedPayload,
                deepLink = map[KEY_DEEP_LINK],
                priority = map[KEY_PRIORITY]?.let {
                    try {
                        Priority.valueOf(it.uppercase())
                    } catch (_: IllegalArgumentException) {
                        Priority.DEFAULT
                    }
                } ?: Priority.DEFAULT,
                sentAt = map[KEY_SENT_AT]?.toLongOrNull(),
                createdAt = map[KEY_CREATED_AT]?.toLongOrNull(),
            )
        }
    }
}