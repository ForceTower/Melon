# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# ------------------------ Timber ------------------------------
# This removes all the logging using timber.d and timber.v
# note that timber.e is kept in code
#-assumenosideeffects class timber.log.Timber {
#    public static *** v(...);
#    public static *** d(...);
#    public static *** i(...);
#}

# Work around android fragment artifact bug
-keep class androidx.navigation.fragment.NavHostFragment { *; }

# Dynamic Features Reflection Calls
-keep class * extends androidx.databinding.DataBinderMapper { *; }

-keepnames class * implements com.forcetower.core.interfaces.DynamicDataSourceFactoryProvider

## Bypass
-keep class in.uncod.android.bypass.Document { <init>(...); }
-keep class in.uncod.android.bypass.Element {
    <init>(...);
    void setChildren(...);
    void setParent(...);
    void addAttribute(...);
}

## Models
-keep class com.forcetower.uefs.core.model.** { *; }

## Gson
-keepattributes Signature
-dontwarn sun.misc.**
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

-dontwarn okhttp3.internal.platform.ConscryptPlatform
-dontwarn org.conscrypt.ConscryptHostnameVerifier

-repackageclasses forcetower

