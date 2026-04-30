# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# Keep annotation attributes, otherwise Jetpack libraries fail
-keepattributes *Annotation*
# Keep DataStore internal classes (prevents release crashes)
-keep class androidx.datastore.** { *; }

# Keep LiveData and ViewModel internals (required)
-keep class androidx.lifecycle.** { *; }
# --- General Android & Debugging ---
# Keeps line numbers in stack traces (Essential for debugging release crashes)
-keepattributes SourceFile,LineNumberTable

# --- Room Database (CRITICAL) ---
# Room uses reflection to load the generated database implementation.
# Without this, you will get "Cannot find implementation for AppDatabase"
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep your Entity classes and DAOs.
# Replace 'com.jatinkumar.sentryone' with your actual package where Entities are.
-keep class com.jatinkumar.sentryone.data.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep class * implements androidx.room.RoomOpenHelper

# --- DataStore & Lifecycle ---
# You already have these, they are good to keep for safety
-keep class androidx.datastore.** { *; }
-keep class androidx.lifecycle.** { *; }

# --- Kotlin Coroutines ---
# Prevents stripping of internal coroutine metadata used at runtime
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext {
    volatile <fields>;
}

# --- Google Play Services (Location) ---
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**

# --- ViewBinding ---
# Prevents R8 from stripping your ViewBinding classes
-keep class com.jatinkumar.sentryone.databinding.** { *; }
-keep class com.jatinkumar.sentryone.Database.EmergencyContact { *; }
# --- App Specific Data Models ---
# Any class used for JSON or saved in Preferences/DataStore should be kept
# to prevent field renaming.
-keep @androidx.annotation.Keep class * { *; }