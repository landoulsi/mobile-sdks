import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
// Android compiles to JVM 11 to match :schemaui (which the survey renders through) and the
// demo/preview apps that consume it.

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

// The Compose wrapper (`Survey()` composable) only exists for the Android/JVM target;
// iOS renders through :schemaui's Swift renderer via [SurveyKit].
composeCompiler {
    targetKotlinPlatforms.set(setOf(KotlinPlatformType.androidJvm))
}

kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.landoulsi.survey"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTest {
            isReturnDefaultValues = true
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.schemaui)
                implementation(projects.logger)
                implementation(projects.timeprovider)
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.core.ktx)
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.ktor.client.okhttp)
                // Compose — explicitly versioned aliases (BOM platform() not available in KMP sourceSets)
                implementation(libs.compose.ui.kmp)
                implementation(libs.compose.foundation.kmp)
                implementation(libs.compose.material3.kmp)
                implementation(libs.compose.ui.tooling.preview.kmp)
            }
        }

        val androidHostTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.junit)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.test.junit)
                implementation(libs.compose.ui.kmp)
                implementation(libs.compose.material3.kmp)
                implementation(libs.compose.ui.test.junit4.kmp)
                implementation(libs.compose.ui.test.manifest.kmp)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        iosTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

group = "com.landoulsi.survey"
version = "1.0.0"
