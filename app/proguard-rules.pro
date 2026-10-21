# Minificación activa en release desde la fase 4 (ver app/build.gradle.kts).
# Lo que no aparece aquí lo resuelven las consumer rules de las propias librerías
# (Compose, kotlinx.coroutines, androidx.lifecycle).

# La librería de WireGuard usa JNI: sin esto, R8 elimina las clases nativas.
-keep class com.wireguard.** { *; }

# ZXing embebido: el launcher de intents y DecoratedBarcodeView se construyen
# por reflexión; sin esto, R8 puede eliminar la activity de captura.
-keep class com.journeyapps.** { *; }
-dontwarn com.google.zxing.**

# Metadatos que Compose y las librerías leen en runtime.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# Anotaciones JSR-305 referenciadas por WireGuard (javax.annotation.Nonnull y
# javax.annotation.meta.TypeQualifierDefault). No están en el classpath y su
# retención es CLASS: no se leen en runtime, así que se silencian.
-dontwarn javax.annotation.**
