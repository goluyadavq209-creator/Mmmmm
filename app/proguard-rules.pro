# ClipGenius AI Release ProGuard / R8 Rules

# Keep WebView JavaScript Bridge methods intact in release builds
-keepattributes *Annotation*
-keepclassmembers class com.example.ui.screens.ClipGeniusJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Firestore data models
-keep @androidx.annotation.Keep class * { *; }
-keep class com.example.data.model.** { *; }

# Preserve source file and line numbers for clearer crash diagnostics
-keepattributes SourceFile,LineNumberTable
