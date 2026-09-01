package com.landoulsi.socialauth.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val socialAuthIoDispatcher: CoroutineDispatcher = Dispatchers.IO
