# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK.

# Keep data model classes for Gson
-keep class com.nexent.app.data.model.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Gson
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
