plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    // ELIMINA O COMENTA LA SIGUIENTE LÍNEA TAMBIÉN:
    // id ("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.controlsalasapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.controlsalasapp"
        minSdk = 26
        targetSdk = 34 // Mantén esta versión, es compatible con compileSdk 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables{
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true // ¡Esto es lo que habilita Jetpack Compose!
    }

    composeOptions {
        // MUY IMPORTANTE: La versión de kotlinCompilerExtensionVersion debe ser COMPATIBLE
        // con tu versión de Kotlin (1.9.0 en tu caso).
        // Para Kotlin 1.9.0, se recomienda 1.5.4 o 1.5.8. La 1.5.11 que tienes es más para Kotlin 2.0.0-RC1
        kotlinCompilerExtensionVersion = "1.5.4" // Cambiar a una versión compatible con Kotlin 1.9.0
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // BOM (Bill Of Materials) para Compose. Ayuda a gestionar las versiones compatibles.
    // Consulta las últimas versiones en: https://developer.android.com/jetpack/compose/bom
    // Usa una versión más reciente, como 2024.05.00 para compatibilidad y mejoras.
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))

    // Core Compose libraries
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Android KTX, Lifecycle, Activity Compose
    implementation ("androidx.core:core-ktx:1.13.1") // Actualizar a la última estable
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0") // Actualizar a la última estable
    implementation ("androidx.activity:activity-compose:1.9.0") // Actualizar a la última estable


    // Compose Navigation
    implementation ("androidx.navigation:navigation-compose:2.7.7") // Esta versión está bien

    // Firebase Authentication
    implementation(platform("com.google.firebase:firebase-bom:32.8.0")) // Esta versión está bien
    implementation ("com.google.firebase:firebase-auth-ktx")

    // Firebase Realtime Database
    implementation ("com.google.firebase:firebase-database-ktx")

    // Coroutines para Firebase y Dispatchers.Main
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3") // Actualizar a la última estable
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // Actualizar a la última estable

    // Gson (para serializar/deserializar el objeto Sala)
    implementation ("com.google.code.gson:gson:2.10.1") // Esta versión está bien

    // Dependencia para monitorear conectividad de red (para NetworkMonitor ViewModel)
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.8.0") // Actualizar a la última estable (o simplemente lifecycle-runtime-ktx si es suficiente)
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0") // Actualizar a la última estable

    implementation ("androidx.compose.material:material-icons-extended:1.6.7") // Actualizar a la última para Compose 1.6+
    implementation ("androidx.compose.runtime:runtime-livedata:1.6.7") // Actualizar a la última para Compose 1.6+

    // Testing
    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.1.5")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00")) // Actualizar la versión del BOM aquí también
    androidTestImplementation ("androidx.compose.ui:ui-test-junit4")
    debugImplementation ("androidx.compose.ui:ui-tooling")
    debugImplementation ("androidx.compose.ui:ui-test-manifest")
}