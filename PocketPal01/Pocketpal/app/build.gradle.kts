import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization")
}

// RF-A2: secretos fuera del código. Se leen de local.properties (ignorado por
// Git) o del entorno, y se exponen vía BuildConfig. Ver local.properties.example.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(name: String): String = localProps.getProperty(name) ?: System.getenv(name) ?: ""

android {
    namespace = "com.pocketpal.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.pocketpal.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"${secret("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${secret("SUPABASE_KEY")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${secret("GOOGLE_WEB_CLIENT_ID")}\"")
        buildConfigField("String", "TERMS_URL", "\"${secret("TERMS_URL")}\"")
        buildConfigField("String", "PRIVACY_URL", "\"${secret("PRIVACY_URL")}\"")
        buildConfigField("String", "SUPPORT_EMAIL", "\"${secret("SUPPORT_EMAIL")}\"")
        buildConfigField("String", "FACEBOOK_URL", "\"${secret("FACEBOOK_URL")}\"")
        buildConfigField("String", "INSTAGRAM_URL", "\"${secret("INSTAGRAM_URL")}\"")
        buildConfigField("String", "X_URL", "\"${secret("X_URL")}\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // RF-A3: AppCompat hace falta para AppCompatDelegate.setApplicationLocales (idioma en ejecución)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    // Fase 6: tests de ViewModels con repositorios simulados (RNF-02, §10)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:3.5.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    // Fase 2: Postgrest para leer/escribir las tablas con RLS
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    // Fase 3: Storage para la foto de perfil (D-09: bucket público avatars)
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:functions-kt")
    implementation("io.ktor:ktor-client-okhttp:3.1.3")

// Navegación y lifecycle
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

// Fase 2: preferencias locales (idioma, tema, moneda…) — RNF-06
    implementation("androidx.datastore:datastore-preferences:1.1.1")

// Fase 3: splash del sistema (RF-A4/D-16) con compat hasta API 23 (minSdk 26)
    implementation("androidx.core:core-splashscreen:1.0.1")

// Fase 3: rotación EXIF de la foto de perfil (la clase del framework la desaconseja lint)
    implementation("androidx.exifinterface:exifinterface:1.4.1")

// Fase 3: foto de perfil (Coil 2.7.0: un solo artefacto y usa OkHttp ya presente;
// Coil 3 cambia de paquete y exige un módulo de red aparte)
    implementation("io.coil-kt:coil-compose:2.7.0")

// Fase 6: reordenar categorías arrastrando (D-10, §7.6: sh.calvin.reorderable)
    implementation("sh.calvin.reorderable:reorderable:3.1.0")

// Íconos
    implementation("androidx.compose.material:material-icons-extended")

// Google Sign-In
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
}