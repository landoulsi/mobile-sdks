pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "mobile-sdks"

include(":demo:app")
include(":payment:app")
include(":payment:shared")
include(":update:app")
include(":update:shared")
include(":design")
include(":logger")
include(":location")
include(":biometric")
include(":storage")
include(":tutorial")
include(":socialauth")
include(":survey")
include(":pushnotification")
include(":document")
include(":analytics")
include(":remoteconfig")
include(":screenshot")
include(":timeprovider")
include(":schemaui")
include(":schemaui:previewApp")
include(":fraud")
include(":viewmodel")
include(":diagnostic")
