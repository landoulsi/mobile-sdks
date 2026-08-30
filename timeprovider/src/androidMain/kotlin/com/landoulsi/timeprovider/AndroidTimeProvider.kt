package com.landoulsi.timeprovider

import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import com.google.android.gms.time.TrustedTime
import com.google.android.gms.time.TrustedTimeClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.time.Clock
import java.util.concurrent.atomic.AtomicReference

/**
 * Android implementation of [TimeProvider] utilizing Google Play Services Trusted Time SDK
 * (`play-services-time`) for cryptographically secure and tamper-resistant network timestamps,
 * with fallback to Android Platform Network Clock (API 33+) or System Clock.
 */
class AndroidTimeProvider(
    initialClient: TrustedTimeClient? = null,
) : TimeProvider {

    private val trustedTimeClientRef = AtomicReference<TrustedTimeClient?>(initialClient)

    constructor(context: Context) : this(null) {
        initializeAsync(context.applicationContext)
    }

    override fun currentTimeMillis(): Long {
        // 1. Try Google Play Services TrustedTimeClient
        trustedTimeClientRef.get()
            ?.runCatching { computeCurrentUnixEpochMillis() }
            ?.getOrNull()
            ?.takeIf { it > 0L }
            ?.let { return it }

        // 2. Try Android SDK Network Time Clock (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { Api33NetworkClockHolder.clock.millis() }
                .getOrNull()
                ?.takeIf { it > 0L }
                ?.let { return it }
        }

        // 3. Fallback to System Clock
        return System.currentTimeMillis()
    }

    /**
     * Initializes the Google Play Services TrustedTime client asynchronously.
     */
    fun initializeAsync(context: Context) {
        runCatching {
            TrustedTime.createClient(context.applicationContext)
                .addOnSuccessListener { client ->
                    trustedTimeClientRef.set(client)
                }
        }
    }

    /**
     * Suspending initialization ensuring TrustedTime client is ready before returning.
     * Propagates [CancellationException] for structured concurrency.
     */
    suspend fun initialize(context: Context): Boolean = try {
        val client = TrustedTime.createClient(context.applicationContext).await()
        trustedTimeClientRef.set(client)
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private object Api33NetworkClockHolder {
        val clock: Clock by lazy { SystemClock.currentNetworkTimeClock() }
    }
}
