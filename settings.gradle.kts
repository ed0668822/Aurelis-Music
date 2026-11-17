// Aurelis © 2026. GPL-3.0-or-later.

pluginManagement {
    repositories {
        google()
        // Prefer the public Maven Central mirror to reduce direct Maven Central 429 requests.
        // Gradle still validates downloaded artifact checksums using its dependency cache.
        maven("https://maven-central-eu.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Prefer the public Maven Central mirror to reduce direct Maven Central 429 requests.
        // Gradle still validates downloaded artifact checksums using its dependency cache.
        maven("https://maven-central-eu.storage-download.googleapis.com/maven2/")
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "Aurelis Music"
include(":app")
