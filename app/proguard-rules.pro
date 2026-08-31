# Supabase / Ktor
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-keep class kotlinx.serialization.** { *; }
-keepattributes *Annotation*, Signature
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Compose
-dontwarn androidx.compose.**

# itext
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# Apache POI
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**

# App models
-keep class com.sevenpro.management.data.model.** { *; }
