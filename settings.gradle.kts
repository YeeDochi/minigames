pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "miniGames"

include(":modules:KKUTU")
include(":modules:Oh_Mock")
include(":modules:catchMind")
include(":modules:Yacht_Dice")
include(":modules:indian_poker")
include("templates")