# Add project specific ProGuard rules here.
# By default, Hilt and Room library configs include their own ProGuard rules.

# Google MediaPipe rules (preserve JNI bindings)
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# Room Database schemas & query entities
-keep class com.posepilot.app.data.db.** { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Jetpack Preferences DataStore
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Hilt configuration components
-keep class com.posepilot.app.di.** { *; }

# General code keep settings
-keepattributes Signature,InnerClasses,EnclosingMethod,AnnotationDefault
