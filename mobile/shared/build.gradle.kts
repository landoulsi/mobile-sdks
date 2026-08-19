plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.landoulsi.payment.shared"
        compileSdk = 37
        minSdk = 24

        withHostTest {
            isReturnDefaultValues = true
        }

        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                // Ktor common dependencies
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)
                // kotlinx serialization
                implementation(libs.kotlinx.serialization.json)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                // Ktor MockEngine for testing
                implementation(libs.ktor.client.mock)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.play.services.wallet)
                implementation(libs.kotlinx.coroutines.play.services)
                implementation(libs.androidx.activity)
                // Ktor OkHttp engine for Android
                implementation(libs.ktor.client.okhttp)
            }
        }
        val androidHostTest by getting {
            dependencies {
                implementation(libs.json)
                implementation(libs.ktor.client.mock)
            }
        }
        iosMain {
            dependencies {
                // Ktor Darwin (URLSession) engine for iOS
                implementation(libs.ktor.client.darwin)
            }
        }
    }

    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }
}
