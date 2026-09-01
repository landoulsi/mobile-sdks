package com.landoulsi.survey.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class AnswerCodecTest {

    @Test
    fun round_trips_a_value_list() {
        val values = listOf("ios", "web")
        assertEquals(values, decodeValueList(encodeValueList(values)))
    }

    @Test
    fun empty_list_round_trips() {
        assertEquals(emptyList(), decodeValueList(encodeValueList(emptyList())))
    }

    @Test
    fun null_and_blank_decode_to_empty() {
        assertEquals(emptyList(), decodeValueList(null))
        assertEquals(emptyList(), decodeValueList(""))
        assertEquals(emptyList(), decodeValueList("   "))
    }

    @Test
    fun bare_scalar_decodes_as_single_element() {
        assertEquals(listOf("dev"), decodeValueList("dev"))
    }

    @Test
    fun malformed_json_decodes_as_single_element_rather_than_throwing() {
        assertEquals(listOf("[oops"), decodeValueList("[oops"))
    }
}
