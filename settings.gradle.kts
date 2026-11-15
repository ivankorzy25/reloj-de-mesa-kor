pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "StandBy"

// :core = la lógica pura en Kotlin/JVM (sin API de Android); :app = todo lo que depende de Android.
include(":core", ":app")
