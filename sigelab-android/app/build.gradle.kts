import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// local.properties no se sube a Git: cada quien puede apuntar a su PC sin tocar este archivo.
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
fun urlLocal(clave: String, porDefecto: String): String = propiedadesLocales.getProperty(clave, porDefecto)

android {
    namespace = "com.universidad.sigelab"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.universidad.sigelab"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            // Con el celular por USB, `adb reverse tcp:8000 tcp:8000` hace que el localhost del teléfono
            // llegue al PC. Las rutas /face/* también van a la API móvil: el servicio facial (8765) no tiene
            // autenticación y solo debe llamarlo esa API desde el mismo PC.
            // Para otra dirección: sigelab.api.url / sigelab.facial.url en local.properties.
            buildConfigField("String", "SIGELAB_API_URL", "\"${urlLocal("sigelab.api.url", "http://localhost:8000/api/")}\"")
            buildConfigField("String", "FACIAL_API_URL", "\"${urlLocal("sigelab.facial.url", "http://localhost:8000/")}\"")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "SIGELAB_API_URL", "\"https://sigelab.ejemplo.edu/api/\"")
            buildConfigField("String", "FACIAL_API_URL", "\"https://sigelab.ejemplo.edu/\"")
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.work.runtime)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.zxing.core)
    implementation(libs.androidx.print)

    testImplementation(libs.junit)
}
