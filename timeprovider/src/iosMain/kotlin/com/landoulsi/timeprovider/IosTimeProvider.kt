package com.landoulsi.timeprovider

/**
 * iOS implementation of [TimeProvider] returning wall-clock epoch timestamp.
 */
class IosTimeProvider : TimeProvider {

    override fun currentTimeMillis(): Long = systemEpochMillis()
}
