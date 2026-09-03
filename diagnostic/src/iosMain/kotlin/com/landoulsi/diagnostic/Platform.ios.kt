package com.landoulsi.diagnostic

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun platform() = "iOS"
actual fun platformTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()