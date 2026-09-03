import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    android {
        namespace = "com.landoulsi.update"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = 24

        withHostTest {
            isReturnDefaultValues = true
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "update"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime) // Required for Compose compiler
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        androidMain {
            dependencies {
                implementation(libs.play.app.update.ktx)
                implementation(libs.androidx.activity.compose)
                implementation("androidx.core:core-ktx:1.13.1")
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.browser)
                
                // Compose UI for Android
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.uiTooling)
            }
        }
    }
}
