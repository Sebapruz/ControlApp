// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id ("com.android.application") version "8.1.0" apply false
    id ("org.jetbrains.kotlin.android") version "1.9.20" apply false // Mantener esta versión
    id ("com.google.gms.google-services")version "4.4.1" apply false
    // ELIMINA O COMENTA LA SIGUIENTE LÍNEA, NO ES NECESARIA PARA JETPACK COMPOSE EN ANDROID:
    // id ("org.jetbrains.kotlin.plugin.compose") version "1.5.11" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        // No necesitas repositorios adicionales aquí si solo usas Jetpack Compose y Firebase
    }
    dependencies {
        classpath("com.google.gms:google-services:4.4.1")
    }
}