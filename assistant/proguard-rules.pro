# Reglas de ProGuard/R8 para JARVIS.
# La app no usa ofuscación por defecto; se deja el fichero para builds de release.
-keep class com.example.jarvis.data.** { *; }
