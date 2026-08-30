package com.trackmit.location

import platform.Foundation.NSDate

class IosTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long {
        val secondsSince2001 = NSDate().timeIntervalSinceReferenceDate
        val secondsSince1970 = secondsSince2001 + 978307200
        return (secondsSince1970 * 1000).toLong()
    }
}
