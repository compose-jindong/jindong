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
include(":jindong-core")
include(":jindong-compose")
include(
    ":sample:shared",
    ":sample:android-app"
)
