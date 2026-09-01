package com.landoulsi.socialauth.oauth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecureRandomTest {

    @Test
    fun returnsRequestedLength() {
        assertEquals(32, secureRandomBytes(32).size)
        assertEquals(1, secureRandomBytes(1).size)
    }

    @Test
    fun zeroLengthIsEmpty() {
        assertTrue(secureRandomBytes(0).isEmpty())
    }

    @Test
    fun successiveCallsDiffer() {
        assertFalse(secureRandomBytes(32).contentEquals(secureRandomBytes(32)))
    }

    @Test
    fun outputIsNotAllZeros() {
        assertTrue(secureRandomBytes(32).any { it.toInt() != 0 })
    }
}
