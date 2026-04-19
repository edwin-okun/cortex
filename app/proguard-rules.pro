# Keep Room entities intact
-keep class com.cortex.app.data.local.entity.** { *; }
# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
