package com.landoulsi.socialauth.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Kotlin/Native has no public `Dispatchers.IO` (it is `internal` in kotlinx-coroutines).
// Native's `Dispatchers.Default` is a multi-thread worker pool, not a CPU-count-sized
// pool as on the JVM, so brief blocking on keychain/file I/O here does not starve it.
internal actual val socialAuthIoDispatcher: CoroutineDispatcher = Dispatchers.Default
