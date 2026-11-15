// Kotlin/JVM puro: acá no se puede usar el SDK de Android, y eso es a propósito.
// Todo lo que se pueda probar con un unit test sin teléfono vive en este módulo.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
