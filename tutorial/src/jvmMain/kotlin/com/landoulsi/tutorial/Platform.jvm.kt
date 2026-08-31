package com.landoulsi.tutorial

actual fun platform() = "Desktop"

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
