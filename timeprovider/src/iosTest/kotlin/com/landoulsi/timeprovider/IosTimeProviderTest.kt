package com.landoulsi.timeprovider

import kotlin.test.Test
import kotlin.test.assertTrue

class IosTimeProviderTest {

    @Test
    fun testIosTimeProviderReturnsPositiveTimestamp() {
        val provider = IosTimeProvider()
        val time = provider.currentTimeMillis()
        assertTrue(time > 0L)
    }
}
