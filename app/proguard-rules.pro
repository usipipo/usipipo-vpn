# Fase 0: minificación desactivada en release. Si se activa más adelante,
# la librería de WireGuard usa JNI y necesita estas reglas.
-keep class com.wireguard.** { *; }
