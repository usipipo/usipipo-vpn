plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Firma de la release. Las cuatro variables de entorno las inyecta GitHub Actions al
// publicar una release (ver .github/workflows/release.yml); el keystore viaja en base64
// dentro de un secreto y nunca se commitea. Si las variables no existen (build local, PR,
// disparo manual del workflow), la variante release se compila SIN firmar.
private data class ReleaseSigning(
    val keystoreFile: String,
    val keystorePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

private val releaseSigning: ReleaseSigning? = run {
    val store = System.getenv("ANDROID_KEYSTORE_FILE")
    val storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
    val alias = System.getenv("ANDROID_KEY_ALIAS")
    val keyPass = System.getenv("ANDROID_KEY_PASSWORD")
    if (!store.isNullOrEmpty() && !storePassword.isNullOrEmpty() &&
        !alias.isNullOrEmpty() && !keyPass.isNullOrEmpty()
    ) {
        ReleaseSigning(store, storePassword, alias, keyPass)
    } else {
        null
    }
}

android {
    namespace = "com.usipipo.vpn"
    compileSdk = 37

    signingConfigs {
        releaseSigning?.let { signing ->
            create("release") {
                storeFile = file(signing.keystoreFile)
                storePassword = signing.keystorePassword
                keyAlias = signing.keyAlias
                keyPassword = signing.keyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.usipipo.vpn"
        minSdk = 26
        targetSdk = 36
        // La release derivada de una etiqueta inyecta -PVERSION_NAME / -PVERSION_CODE;
        // sin etiqueta se mantienen los valores por defecto de la fase 0.
        versionCode = (project.findProperty("VERSION_CODE") as? String)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("VERSION_NAME") as? String) ?: "0.1.0"
    }

    buildTypes {
        release {
            if (releaseSigning != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            // Fase 0: sin minificación. Se activa en una fase posterior (ver proguard-rules.pro).
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // La librería tunnel de WireGuard pide desugaring de la librería central.
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.kotlinx.coroutines.android)

    // Motor del túnel (librería oficial de WireGuard para Android)
    implementation(libs.wireguard.tunnel)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Escáner QR offline (no depende de Google Play Services)
    implementation(libs.zxing.embedded)
    implementation(libs.zxing.core)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
