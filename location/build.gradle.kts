plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

kotlin {
    jvmToolchain(17)
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.logger)
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            // Ktor client for the IP-based approximate location provider
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }

        androidMain.dependencies {
            // api: FusedLocationProviderClient is constructed by shared's AndroidModule
            // (needs a Context, which only shared's Metro graph supplies) and passed into
            // FusedLocationProvider here, so the type must be visible to shared.
            api(libs.google.play.location)
            implementation(libs.google.play.time)
            // await() on FusedLocationProviderClient.lastLocation Task
            implementation(libs.kotlinx.coroutines.play.services)
            // Ktor OkHttp engine for the IP-based approximate location provider
            implementation(libs.ktor.client.okhttp)
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        iosTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.trackflow.location"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
