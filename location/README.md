# `:location`

Device location for Kotlin Multiplatform (Android + iOS), with a permission-free
IP fallback.

## What's here

| Type | File | Role |
| --- | --- | --- |
| `LocationProvider` | `commonMain/.../LocationProvider.kt` | `startTracking` / `stopTracking` / `locationUpdates(): Flow<Location>` / `suspend lastKnownLocation()`. |
| `Location` | `commonMain/.../Location.kt` | `latitude`, `longitude`, optional `accuracy`/`speed`/`bearing`, RFC 3339 `timestamp`. |
| `IpLocationProvider` | `commonMain/.../IpLocationProvider.kt` | One-shot, coarse (~50 km) city-level fix from the public IP. Needs network only — **no** location permission. `locationUpdates()` emits at most one fix then completes. |
| `IpLocationConfig` | `commonMain/.../IpLocationProvider.kt` | `endpointUrl` (HTTPS, defaults to keyless `ipwho.is`), `approximateAccuracyMeters`. |
| `FusedLocationProvider` / `GpsLocationProvider` | `androidMain` | Real permission-backed providers (Play Services fused, framework GPS). |
| `IosLocationProvider` | `iosMain` | `CLLocationManager`-backed provider. |
| `TimeProvider` + `TimeProvider.currentTimestamp()` | `commonMain/.../TimeProvider.kt` | Module-local clock that formats the one wire timestamp (ISO-8601 UTC, ms). Distinct from the separate `:timeprovider` module. |

## Usage

```kotlin
// before the permission prompt: approximate starting position, no permission
val provider = IpLocationProvider(timeProvider)          // owns its HttpClient
val approx: Location? = provider.lastKnownLocation()
provider.close()

// after permission granted: prefer a real provider
fused.startTracking()
fused.locationUpdates().collect { fix -> /* ... */ }
```

## Notes

- `IpLocationProvider` does **not** refresh on a timer — an IP fix only moves when
  network egress does. Call `invalidateCache()` after a Wi-Fi/cellular/VPN switch.
- The default keyless endpoint sends the client IP to a third party and logs a
  one-time warning; point `endpointUrl` at a first-party proxy for production.

## Tests

```bash
./gradlew :location:allTests
```
