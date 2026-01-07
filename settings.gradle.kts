pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "jindong"
include(":jindong")
include(
    ":sample:shared",
    ":sample:android-app"
)
