package com.landoulsi.tutorial

actual fun platform() = "Android"

actual fun currentTimeMillis(): Long = System.currentTimeMillis()