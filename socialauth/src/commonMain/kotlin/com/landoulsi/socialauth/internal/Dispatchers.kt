package com.landoulsi.socialauth.internal

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dispatcher for the SDK's blocking work — session-store reads/writes (disk, keystore,
 * `EncryptedSharedPreferences`) and, transitively, token-endpoint calls.
 *
 * Android maps this to `Dispatchers.IO`; iOS to `Dispatchers.Default` (Kotlin/Native has
 * no public `Dispatchers.IO`, and its `Default` pool tolerates brief blocking). Not
 * referenced directly from `commonMain` because `Dispatchers.IO` is not visible there.
 */
internal expect val socialAuthIoDispatcher: CoroutineDispatcher
