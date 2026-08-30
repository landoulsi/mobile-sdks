package com.landoulsi.analytics

import kotlin.test.Test
import kotlin.test.assertEquals

class EventMapperTest {

    @Test
    fun string_value_maps_to_string() {
        val event = Event(eventName = "test", properties = mapOf("key" to AnalyticsValue.String("hello")))
        val flat = EventMapper.toFlatMap(event)
        assertEquals("hello", flat["key"])
    }

    @Test
    fun long_value_maps_to_long() {
        val event = Event(eventName = "test", properties = mapOf("count" to AnalyticsValue.Long(42L)))
        val flat = EventMapper.toFlatMap(event)
        assertEquals(42L, flat["count"])
    }

    @Test
    fun double_value_maps_to_double() {
        val event = Event(eventName = "test", properties = mapOf("ratio" to AnalyticsValue.Double(3.14)))
        val flat = EventMapper.toFlatMap(event)
        assertEquals(3.14, flat["ratio"])
    }

    @Test
    fun boolean_value_maps_to_boolean() {
        val event = Event(eventName = "test", properties = mapOf("enabled" to AnalyticsValue.Boolean(true)))
        val flat = EventMapper.toFlatMap(event)
        assertEquals(true, flat["enabled"])
    }

    @Test
    fun sensitive_key_redacted() {
        val event = Event(eventName = "test", properties = mapOf("email" to AnalyticsValue.String("user@test.com")))
        val flat = EventMapper.toFlatMap(event)
        assertEquals("[REDACTED]", flat["email"])
    }

    @Test
    fun sensitive_key_prefix_redacted() {
        val event = Event(eventName = "test", properties = mapOf("card_last4" to AnalyticsValue.String("4242")))
        val flat = EventMapper.toFlatMap(event)
        assertEquals("[REDACTED]", flat["card_last4"])
    }

    @Test
    fun non_sensitive_key_passes_through() {
        val event = Event(eventName = "test", properties = mapOf("safe_key" to AnalyticsValue.String("visible")))
        val flat = EventMapper.toFlatMap(event)
        assertEquals("visible", flat["safe_key"])
    }

    @Test
    fun empty_properties_returns_empty_flat_map() {
        val event = Event(eventName = "test", timestamp = 0L)
        val flat = EventMapper.toFlatMap(event)
        assertEquals(0, flat.size)
    }

    @Test
    fun case_insensitive_sensitive_key_redaction() {
        val event = Event(eventName = "test", properties = mapOf("Email" to AnalyticsValue.String("a@b.com")))
        val flat = EventMapper.toFlatMap(event)
        assertEquals("[REDACTED]", flat["Email"])
    }
}