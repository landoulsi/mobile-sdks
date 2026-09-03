package com.landoulsi.diagnostic

actual fun platform() = "Android"
actual fun platformTimeMillis(): Long = System.currentTimeMillis()