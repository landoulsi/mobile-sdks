package com.trackflow.logger

import timber.log.Timber

actual object Logger {
    actual fun v(tag: String, message: String) {
        Timber.tag(tag).v(message)
    }

    actual fun d(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }

    actual fun i(tag: String, message: String) {
        Timber.tag(tag).i(message)
    }

    actual fun w(tag: String, message: String) {
        Timber.tag(tag).w(message)
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        Timber.tag(tag).e(throwable, message)
    }
}
