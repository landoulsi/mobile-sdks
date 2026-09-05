package com.landoulsi.integrity

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Android `*CheckContext` implementations need Robolectric (unused repo-wide) to behave
 * usefully here; evaluator/engine/config coverage instead lives in `commonTest` against
 * the platform-agnostic `*CheckContext` interfaces via fakes.
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}