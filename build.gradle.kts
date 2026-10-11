// Proyecto raíz: solo declara los plugins; se aplican en :app.
// Con AGP 9.x, Kotlin viene integrado: no se aplica org.jetbrains.kotlin.android.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
