import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}
// tabla clave-valor para parsear archivos .properties
val localProperties = Properties()  // Ruta física al archivo local.properties en la raíz del proyecto
val localPropertiesFile = rootProject.file("local.properties")
//validar si el archivo existe antes de intentar leerlo
if(localPropertiesFile.exists()) { localProperties.load(localPropertiesFile.inputStream()) }

// lectura con ?: para tolerar los fallos
// busca la clave en local.properties, Si es nula, busca en las variables de entorno del sistema operativo con System.getenv
// Si tampoco existe, asigna una cadena vacía "" para que Gradle no lance NullPointerException

// Función auxiliar para leer propiedades
fun getEnvProperty(key: String): String {
    val raw = localProperties.getProperty(key) ?: System.getenv(key) ?: ""
    return raw.trim().removeSurrounding("\"")
}
// si no se definine Cloud, o Phone en SUPABASE_ENV, toma por defecto el local
val activeEnv = getEnvProperty("SUPABASE_ENV").ifEmpty { "LOCAL" }.uppercase()

// Se pasa por parametro la variable de entorno que se necesita, la cual viene de local.properties
val (activeUrl, activeKey) = when (activeEnv)
{
    "CLOUD" -> Pair(
        getEnvProperty("SUPABASE_URL_CLOUD")
            .ifEmpty { getEnvProperty("SUPABASE_URL") },
        getEnvProperty("SUPABASE_ANON_KEY_CLOUD")
            .ifEmpty { getEnvProperty("SUPABASE_ANON_KEY") }
    )
    "PHONE" -> Pair(
        getEnvProperty("SUPABASE_URL_PHONE"),
        getEnvProperty("SUPABASE_ANON_KEY_PHONE")
    )
    else -> Pair( // Local por defecto
        getEnvProperty("SUPABASE_URL_LOCAL"),
        getEnvProperty("SUPABASE_ANON_KEY_LOCAL")
    )
}

android {
    namespace = "com.ecoruta.app"
    compileSdk = 36 //the version on which the app will run

    defaultConfig {
        applicationId = "com.ecoruta.app"
        minSdk = 24 //minima version funcional en android
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inyección de variables seguras para el cliente Supabase
        buildConfigField("String", "SUPABASE_URL", "\"$activeUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$activeKey\"")
        buildConfigField("String", "SUPABASE_ENV", "\"$activeEnv\"") // Para saber en logs cuál está activo
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true //habilitar BuildConfig
    }
}

// exportación del esquema de Room para las migraciones de la base de datos
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // 1. Android Core and Jetpack Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Dependency injection `hilt` and navigation
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)

    // Supabase SDK Auth, Database, Realtime, Storage y Ktor Engine
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.cio)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.supabase.functions)

    //Maps & GPS: Real-Time location tracking for drivers
    implementation(libs.google.maps.compose)
    implementation(libs.play.services.location)

    //Camera and QR code scanner
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    //Local Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    //WorkManager: Driver GPS Tracking in the Background
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)

    //UI Features: Image Uploading and Location/Camera Permissions
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}