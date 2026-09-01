# `:logger`

Minimal cross-platform logging façade (Android + iOS).

## API

`commonMain/.../Logger.kt`:

```kotlin
expect object Logger {
    fun v(tag: String, message: String)
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
```

- `androidMain` — delegates to Timber (`Timber.tag(tag).d(...)`).
- `iosMain` — writes to stdout via `println` with a level prefix.

## Usage

```kotlin
Logger.d("Checkout", "payment sheet opened")
Logger.e("Checkout", "tokenization failed", err)
```

Other modules in this repo (e.g. `:location`) depend on this for log output.
Nothing to configure — it's a stateless `object`.
