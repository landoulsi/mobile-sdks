package com.landoulsi.tutorial.storage

import com.landoulsi.tutorial.model.TutorialProgress
import com.russhwolf.settings.Settings
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Storage abstraction for persisting tutorial progress.
 */
interface TutorialStorage {
    /**
     * Retrieves the progress for a specific tutorial ID, or null if none exists.
     */
    suspend fun getProgress(tutorialId: String): TutorialProgress?

    /**
     * Retrieves all saved tutorial progress mapped by tutorial ID.
     */
    suspend fun getAllProgress(): Map<String, TutorialProgress>

    /**
     * Persists or updates the progress for a tutorial.
     */
    suspend fun saveProgress(progress: TutorialProgress)

    /**
     * Removes the progress entry for a specific tutorial ID.
     */
    suspend fun clearProgress(tutorialId: String)

    /**
     * Clears all saved tutorial progress entries.
     */
    suspend fun clearAll()
}

/**
 * [TutorialStorage] implementation backed by [Settings] and [Json] serialization.
 */
class SettingsTutorialStorage(
    private val settings: Settings,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    },
    private val keyPrefix: String = KEY_PREFIX
) : TutorialStorage {

    private val mutex = Mutex()
    private val setSerializer = SetSerializer(String.serializer())

    override suspend fun getProgress(tutorialId: String): TutorialProgress? = mutex.withLock {
        val serialized = settings.getStringOrNull(itemKey(tutorialId)) ?: return null
        return try {
            json.decodeFromString(TutorialProgress.serializer(), serialized)
        } catch (_: Throwable) {
            null
        }
    }

    override suspend fun getAllProgress(): Map<String, TutorialProgress> = mutex.withLock {
        val keysString = settings.getStringOrNull(indexKey()) ?: return emptyMap()
        val ids = try {
            json.decodeFromString(setSerializer, keysString)
        } catch (_: Throwable) {
            emptySet()
        }

        val result = mutableMapOf<String, TutorialProgress>()
        for (id in ids) {
            val serialized = settings.getStringOrNull(itemKey(id)) ?: continue
            try {
                result[id] = json.decodeFromString(TutorialProgress.serializer(), serialized)
            } catch (_: Throwable) {
                // Ignore corrupted item
            }
        }
        result
    }

    override suspend fun saveProgress(progress: TutorialProgress): Unit = mutex.withLock {
        val serialized = json.encodeToString(TutorialProgress.serializer(), progress)
        settings.putString(itemKey(progress.tutorialId), serialized)

        val currentIds = getStoredIdsInternal()
        if (!currentIds.contains(progress.tutorialId)) {
            val updated = currentIds + progress.tutorialId
            settings.putString(indexKey(), json.encodeToString(setSerializer, updated))
        }
    }

    override suspend fun clearProgress(tutorialId: String): Unit = mutex.withLock {
        settings.remove(itemKey(tutorialId))
        val currentIds = getStoredIdsInternal()
        if (currentIds.contains(tutorialId)) {
            val updated = currentIds - tutorialId
            settings.putString(indexKey(), json.encodeToString(setSerializer, updated))
        }
    }

    override suspend fun clearAll(): Unit = mutex.withLock {
        val currentIds = getStoredIdsInternal()
        for (id in currentIds) {
            settings.remove(itemKey(id))
        }
        settings.remove(indexKey())
    }

    private fun getStoredIdsInternal(): Set<String> {
        val keysString = settings.getStringOrNull(indexKey()) ?: return emptySet()
        return try {
            json.decodeFromString(setSerializer, keysString)
        } catch (_: Throwable) {
            emptySet()
        }
    }

    private fun itemKey(tutorialId: String) = "${keyPrefix}item_$tutorialId"
    private fun indexKey() = "${keyPrefix}index"

    companion object {
        const val KEY_PREFIX = "com.landoulsi.tutorial.progress."
    }
}

/**
 * Thread-safe, in-memory implementation of [TutorialStorage] suitable for testing or ephemeral sessions.
 */
class InMemoryTutorialStorage : TutorialStorage {
    private val mutex = Mutex()
    private val memoryStore = mutableMapOf<String, TutorialProgress>()

    override suspend fun getProgress(tutorialId: String): TutorialProgress? = mutex.withLock {
        memoryStore[tutorialId]
    }

    override suspend fun getAllProgress(): Map<String, TutorialProgress> = mutex.withLock {
        memoryStore.toMap()
    }

    override suspend fun saveProgress(progress: TutorialProgress): Unit = mutex.withLock {
        memoryStore[progress.tutorialId] = progress
    }

    override suspend fun clearProgress(tutorialId: String): Unit = mutex.withLock {
        memoryStore.remove(tutorialId)
    }

    override suspend fun clearAll(): Unit = mutex.withLock {
        memoryStore.clear()
    }
}
