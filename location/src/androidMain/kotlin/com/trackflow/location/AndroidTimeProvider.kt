package com.trackflow.location

import android.content.Context
import com.google.android.gms.time.TrustedTime
import com.google.android.gms.time.TrustedTimeClient
import com.trackflow.logger.Logger
import dev.zacsweers.metro.Inject

@Inject
class AndroidTimeProvider constructor(
    context: Context
) : TimeProvider {
    private var trustedTimeClient: TrustedTimeClient? = null

    init {
        TrustedTime.createClient(context)
            .addOnSuccessListener { client ->
                trustedTimeClient = client
            }
            .addOnFailureListener { e ->
                Logger.w("AndroidTimeProvider", "Failed to create TrustedTimeClient")
            }
    }

    override fun currentTimeMillis(): Long =
        trustedTimeClient?.computeCurrentUnixEpochMillis() ?: System.currentTimeMillis()
}
