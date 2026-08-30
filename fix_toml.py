with open('/Users/ahmed/Work/trackmit/mobile/gradle/libs.versions.toml', 'r') as f:
    content = f.read()

versions_to_add = """
playServicesWallet = "19.4.0"
browser = "1.8.0"
json = "20250107"
ktor = "3.1.3"
"""

libraries_to_add = """
play-services-wallet = { group = "com.google.android.gms", name = "play-services-wallet", version.ref = "playServicesWallet" }
androidx-browser = { group = "androidx.browser", name = "browser", version.ref = "browser" }
kotlinx-coroutines-play-services = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "kotlinx-coroutines" }
json = { group = "org.json", name = "json", version.ref = "json" }
ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-logging = { group = "io.ktor", name = "ktor-client-logging", version.ref = "ktor" }
ktor-client-okhttp = { group = "io.ktor", name = "ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { group = "io.ktor", name = "ktor-client-darwin", version.ref = "ktor" }
ktor-client-mock = { group = "io.ktor", name = "ktor-client-mock", version.ref = "ktor" }
kotlin-reflect = { group = "org.jetbrains.kotlin", name = "kotlin-reflect", version.ref = "kotlin" }
"""

plugins_to_add = """
android-kotlin-multiplatform-library = { id = "com.android.kotlin.multiplatform.library", version.ref = "agp" }
"""

# Insert versions before [libraries]
content = content.replace('[libraries]', versions_to_add + '\n[libraries]')
# Insert libraries before [bundles]
content = content.replace('[bundles]', libraries_to_add + '\n[bundles]')
# Append plugins
content = content + plugins_to_add

with open('/Users/ahmed/Work/mobile-sdks/gradle/libs.versions.toml', 'w') as f:
    f.write(content)
