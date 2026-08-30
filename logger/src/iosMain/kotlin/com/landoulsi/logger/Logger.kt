package com.landoulsi.logger

actual object Logger {
    actual fun v(tag: String, message: String) {
        println("VERBOSE: [$tag] $message")
    }

    actual fun d(tag: String, message: String) {
        println("DEBUG: [$tag] $message")
    }

    actual fun i(tag: String, message: String) {
        println("INFO: [$tag] $message")
    }

    actual fun w(tag: String, message: String) {
        println("WARN: [$tag] $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        println("ERROR: [$tag] $message")
        throwable?.printStackTrace()
    }
}
