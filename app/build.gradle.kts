import java.util.Properties

// AGP 9: Kotlin viene adentro del plugin de Android, no hace falta org.jetbrains.kotlin.android.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Firma de release: en CI, desde variables de entorno (GitHub Secrets); en la máquina, desde
 * keystore.properties en la raíz (está en .gitignore). Si no hay ninguno de los dos, el release
 * sale sin firmar. La clave y las contraseñas nunca entran al repositorio.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun signingValue(envName: String, propertyName: String): String? =
    providers.environmentVariable(envName).orNull?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(propertyName)

val releaseStoreFile = signingValue("STANDBY_KEYSTORE_FILE", "storeFile")

// El versionCode sube con el número de corrida de CI, para que la build nueva se instale encima.
val ciVersionCode = providers.environmentVariable("GITHUB_RUN_NUMBER").orNull?.toIntOrNull()
val ciVersionName = providers.environmentVariable("STANDBY_VERSION_NAME").orNull?.removePrefix("v")

android {
    namespace = "com.realfamousbae.standby.dock"
    compileSdk = 37

    defaultConfig {
        applicationId = "ar.kor.reloj"
        minSdk = 31
        // targetSdk = la versión de Android del teléfono de Iván (17), para probar lo real.
        targetSdk = 37
        versionCode = ciVersionCode ?: 2
        versionName = ciVersionName ?: "1.0"
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = signingValue("STANDBY_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("STANDBY_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("STANDBY_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // APK chico para pasárselo a los amigos por WhatsApp: R8 saca lo que no se usa.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
