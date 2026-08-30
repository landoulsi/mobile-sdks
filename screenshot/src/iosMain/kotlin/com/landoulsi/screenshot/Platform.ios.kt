package com.landoulsi.screenshot

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun platform() = "iOS"

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()