package com.trackflow.location

import kotlinx.coroutines.flow.Flow

interface LocationProvider {
    fun startTracking()
    fun stopTracking()
    fun locationUpdates(): Flow<Location>
}
