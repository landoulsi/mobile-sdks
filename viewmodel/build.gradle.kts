import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
}

kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.landoulsi.viewmodel"
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
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // iosX64 (Intel simulator) is intentionally omitted: androidx.lifecycle:lifecycle-viewmodel
    // stopped publishing an iosX64 artifact as of 2.11.0, and Apple-silicon simulators use
    // iosSimulatorArm64. Keeping iosX64 here breaks KMP dependency resolution / Gradle sync.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                api(libs.kotlinx.coroutines.core)
                api(libs.androidx.lifecycle.viewmodel)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        jvmMain {
            dependencies {
            }
        }

        androidMain {
            dependencies {
                api(libs.androidx.lifecycle.runtime.ktx)
                api(libs.androidx.lifecycle.viewmodel.ktx)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.test.junit)
            }
        }

        iosMain {
            dependencies {
            }
        }
    }
}
