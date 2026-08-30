package com.trackmit.location

import com.trackmit.location.network.locationHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val FIXED_MILLIS = 1_778_570_040_000L // 2026-05-12T07:14:00.000Z
private const val FIXED_TIMESTAMP = "2026-05-12T07:14:00.000Z"

private class FakeTimeProvider(private val millis: Long = FIXED_MILLIS) : TimeProvider {
    override fun currentTimeMillis(): Long = millis
}

private fun jsonEngine(body: String) = MockEngine {
    respond(
        content = body,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )
}

private val testConfig = IpLocationConfig(endpointUrl = "https://ipwho.is/")

private fun provider(engine: MockEngine, time: TimeProvider = FakeTimeProvider()) =
    IpLocationProvider(
        httpClient = locationHttpClient(engine),
        timeProvider = time,
        config = testConfig,
    )

@OptIn(ExperimentalCoroutinesApi::class)
class IpLocationProviderTest {

    @Test
    fun lastKnownLocationParsesValidResponse() = runTest(UnconfinedTestDispatcher()) {
        val body = """{"success":true,"latitude":24.4539,"longitude":54.3773,"city":"Abu Dhabi"}"""
        val p = provider(jsonEngine(body))

        val loc = p.lastKnownLocation()!!

        assertEquals(24.4539, loc.latitude)
        assertEquals(54.3773, loc.longitude)
        assertEquals(IP_LOCATION_ACCURACY_METERS, loc.accuracy)
        assertNull(loc.speed)
        assertNull(loc.bearing)
        // Timestamp always comes from the SDK's TimeProvider, never the API payload.
        assertEquals(FIXED_TIMESTAMP, loc.timestamp)
    }

    @Test
    fun lastKnownLocationFetchesOnceThenServesCache() = runTest(UnconfinedTestDispatcher()) {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond(
                content = """{"success":true,"latitude":24.4539,"longitude":54.3773}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val p = provider(engine)

        val first = p.lastKnownLocation()
        val second = p.lastKnownLocation()

        assertEquals(24.4539, first?.latitude)
        assertEquals(first, second)
        assertEquals(1, calls, "second call must be served from cache, not re-fetched")
    }

    @Test
    fun lastKnownLocationRetriesWhileCacheStillEmpty() = runTest(UnconfinedTestDispatcher()) {
        var calls = 0
        val engine = MockEngine {
            calls++
            if (calls == 1) error("simulated network failure")
            respond(
                content = """{"success":true,"latitude":48.8566,"longitude":2.3522}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val p = provider(engine)

        assertNull(p.lastKnownLocation(), "first lookup failed")
        assertEquals(48.8566, p.lastKnownLocation()?.latitude, "empty cache -> retry on next call")
        assertEquals(2, calls)
    }

    @Test
    fun lastKnownLocationReturnsNullWhenLookupFailsWithEmptyCache() = runTest(UnconfinedTestDispatcher()) {
        val p = provider(MockEngine { error("offline") })

        assertNull(p.lastKnownLocation(), "no cached fix and the lookup failed")
    }

    @Test
    fun lastKnownLocationIgnoresErrorBodyReturnedWithHttp200() = runTest(UnconfinedTestDispatcher()) {
        // ipapi.co shape (`reason`)
        assertNull(
            provider(jsonEngine("""{"success":false,"reason":"RateLimited"}""")).lastKnownLocation(),
            "an error body returned with HTTP 200 is not a fix",
        )
        // ipwho.is shape (`message`)
        assertNull(
            provider(jsonEngine("""{"success":false,"message":"Invalid IP address"}""")).lastKnownLocation(),
            "ipwho.is reports failure text under `message`",
        )
    }

    @Test
    fun lastKnownLocationRejectsInvalidCoordinates() = runTest(UnconfinedTestDispatcher()) {
        suspend fun result(body: String) = provider(jsonEngine(body)).lastKnownLocation()

        assertNull(result("""{"latitude":0.0,"longitude":0.0}"""), "(0, 0) is a failed-geocode sentinel")
        assertNull(result("""{"success":true,"latitude":120.0,"longitude":54.0}"""), "latitude > 90")
        assertNull(result("""{"success":true,"latitude":40.0,"longitude":195.0}"""), "longitude > 180")
        assertNull(result("""{"success":true,"latitude":40.0}"""), "missing longitude")
        assertNull(result("""{"success":true}"""), "missing both coordinates")
    }

    @Test
    fun lastKnownLocationReturnsNullOnHttpErrorPage() = runTest(UnconfinedTestDispatcher()) {
        val engine = MockEngine {
            respond(
                content = "<html><body>502 Bad Gateway</body></html>",
                status = HttpStatusCode.BadGateway,
                headers = headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        assertNull(provider(engine).lastKnownLocation(), "a 5xx HTML error page must not crash or parse")
    }

    @Test
    fun locationUpdatesEmitsOnceThenCompletes() = runTest(UnconfinedTestDispatcher()) {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond(
                content = """{"success":true,"latitude":35.6895,"longitude":139.6917}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val p = provider(engine)

        p.startTracking()
        // toList() returns only once the flow completes — proving it emits exactly one value
        // and does not stay open polling.
        val emitted = p.locationUpdates().toList()

        assertEquals(1, emitted.size)
        assertEquals(35.6895, emitted.single().latitude)
        assertEquals(1, calls, "exactly one network request; no periodic polling")
    }

    @Test
    fun locationUpdatesParksUntilStartTracking() = runTest(UnconfinedTestDispatcher()) {
        val p = provider(jsonEngine("""{"success":true,"latitude":35.6,"longitude":139.6}"""))

        val received = mutableListOf<Location>()
        // backgroundScope is auto-cancelled by runTest, so the parked collector is fine to leave.
        backgroundScope.launch { p.locationUpdates().collect { received.add(it) } }

        yield()
        assertTrue(received.isEmpty(), "flow parks until startTracking()")
    }

    @Test
    fun locationUpdatesServesCacheSeededByLastKnownLocation() = runTest(UnconfinedTestDispatcher()) {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond(
                content = """{"success":true,"latitude":1.3521,"longitude":103.8198}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val p = provider(engine)

        val direct = p.lastKnownLocation()
        val streamed = p.locationUpdates().onStart { p.startTracking() }.first()

        assertEquals(direct, streamed)
        assertEquals(1, calls, "the stream reuses the cached fix")
    }

    @Test
    fun locationUpdatesCompletesEmptyWhenFetchFails() = runTest(UnconfinedTestDispatcher()) {
        val p = provider(MockEngine { error("offline") })

        p.startTracking()
        // toList() returning at all proves the flow completed rather than stalling.
        val emitted = p.locationUpdates().toList()

        assertTrue(emitted.isEmpty(), "a failed fetch yields an empty, completed flow")
    }

    @Test
    fun locationUpdatesRetriesOnRecollectionAfterAFailedFetch() = runTest(UnconfinedTestDispatcher()) {
        var calls = 0
        val engine = MockEngine {
            calls++
            if (calls == 1) error("offline at start")
            respond(
                content = """{"success":true,"latitude":52.52,"longitude":13.405}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val p = provider(engine)
        p.startTracking()

        assertTrue(p.locationUpdates().toList().isEmpty(), "first collection: fetch failed")

        val retried = p.locationUpdates().toList()
        assertEquals(listOf(52.52), retried.map { it.latitude }, "re-collecting retries the fetch")
        assertEquals(2, calls)
    }

    @Test
    fun closeDoesNotTouchADiSuppliedHttpClient() = runTest(UnconfinedTestDispatcher()) {
        val client = locationHttpClient(jsonEngine("""{"success":true,"latitude":40.7,"longitude":-74.0}"""))
        val p = IpLocationProvider(client, FakeTimeProvider(), testConfig)
        assertFalse(p.ownsHttpClient, "a client passed in via the constructor is not owned")

        p.close()

        assertTrue(client.isActive, "a constructor-supplied client must stay open after close()")
    }

    @Test
    fun closedProviderServesCacheButDoesNotFetch() = runTest(UnconfinedTestDispatcher()) {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond(
                content = """{"success":true,"latitude":1.0,"longitude":2.0}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val p = provider(engine)

        assertEquals(1.0, p.lastKnownLocation()?.latitude) // fetches and caches
        p.close()

        assertEquals(1.0, p.lastKnownLocation()?.latitude, "cache is still served after close()")
        assertEquals(1, calls, "no new fetch is attempted after close()")
    }

    @Test
    fun invalidateCacheForcesARefetch() = runTest(UnconfinedTestDispatcher()) {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond(
                content = """{"success":true,"latitude":${40 + calls}.0,"longitude":2.0}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val p = provider(engine)

        assertEquals(41.0, p.lastKnownLocation()?.latitude)
        assertEquals(41.0, p.lastKnownLocation()?.latitude, "served from cache")

        p.invalidateCache()
        assertEquals(42.0, p.lastKnownLocation()?.latitude, "re-queried after invalidateCache()")
        assertEquals(2, calls)
    }

    @Test
    fun configRejectsNonHttpsEndpoint() {
        assertFailsWith<IllegalArgumentException> {
            IpLocationConfig(endpointUrl = "http://insecure.example/json")
        }
    }

    @Test
    fun configRejectsNonPositiveOrNonFiniteAccuracy() {
        assertFailsWith<IllegalArgumentException> { IpLocationConfig(approximateAccuracyMeters = 0.0) }
        assertFailsWith<IllegalArgumentException> { IpLocationConfig(approximateAccuracyMeters = -1.0) }
        assertFailsWith<IllegalArgumentException> { IpLocationConfig(approximateAccuracyMeters = Double.NaN) }
        assertFailsWith<IllegalArgumentException> {
            IpLocationConfig(approximateAccuracyMeters = Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun factoryOwnsItsHttpClient() {
        val p = IpLocationProvider(FakeTimeProvider())
        try {
            assertTrue(p.ownsHttpClient, "the no-client factory creates and owns the HttpClient")
        } finally {
            p.close()
        }
    }
}
