pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // No necesitas repositorios adicionales para Compose Multiplatform si no lo usas.
        // Si más adelante usas alguna librería que lo requiera, se añadiría aquí.
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // No necesitas repositorios adicionales para Compose Multiplatform si no lo usas.
        // Si más adelante usas alguna librería que lo requiera, se añadiría aquí.
    }
}

rootProject.name = "ControlSalasApp"
include(":app")