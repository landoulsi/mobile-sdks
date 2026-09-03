plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.android.built.in1.kotlin) apply false
}

// Version used for all published SDK artifacts. Override with -PsdkVersion=X.Y.Z
// (the publish-to-maven-local.sh script passes this automatically).
val sdkVersion: String = (findProperty("sdkVersion") as? String) ?: "1.0.0"

// All SDK modules share a single group; the artifact name is the module name.
allprojects {
    group = "com.landoulsi"
    version = sdkVersion
}

// Configure publishing for every KMP library module. With the new Android KMP
// plugin (com.android.kotlin.multiplatform.library) the Kotlin Multiplatform
// plugin no longer applies maven-publish automatically, so we apply it here.
// App/demo modules use the Android application plugin and are excluded.
subprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        apply(plugin = "maven-publish")
        extensions.configure<PublishingExtension> {
            publications.withType<MavenPublication>().configureEach {
                pom {
                    name.set(project.name)
                    description.set("Landoulsi mobile SDK module: ${project.name}")
                    url.set("https://github.com/landoulsi/mobile-sdks")
                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("landoulsi")
                            name.set("Landoulsi")
                        }
                    }
                    scm {
                        url.set("https://github.com/landoulsi/mobile-sdks")
                    }
                }
            }
        }
    }
}
