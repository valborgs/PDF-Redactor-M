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

# Kotlin Serialization rules are provided by the library automatically.
# Duplicate or outdated rules caused R8 warnings. (Removed)
# Note: You may see a warning "The type \"<1>$*\" is used in a field rule" from kotlinx-serialization-common.pro.
# This is a known benign warning in kotlinx-serialization 1.9.0+ and can be safely ignored.

# PdfBox-Android (Uses heavy reflection)
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.gemalto.jp2.JP2Decoder
-dontwarn com.gemalto.jp2.JP2Encoder

# Network DTOs (Json parsing protection due to missing @SerialName)
-keep class org.comon.pdfredactorm.core.network.dto.** { *; }

# Retrofit & OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-dontwarn okhttp3.**
-dontwarn okio.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.TypeConverter
-keep @androidx.room.Entity class * { *; }