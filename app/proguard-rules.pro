-keepattributes LineNumberTable
-renamesourcefileattribute SourceFile
-repackageclasses unes

# ------------------------ Timber ------------------------------
# This removes all the logging using timber.d and timber.v
# note that timber.e is kept in code
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}

# Work around android fragment artifact bug
-keep class androidx.navigation.fragment.NavHostFragment { *; }
-keep class androidx.navigation.dynamicfeatures.fragment.DynamicNavHostFragment { *; }

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

## About Libraries
-keepclasseswithmembers class **.R$* {
    public static final int define_*;
}

## Models
-keep class com.forcetower.uefs.core.model.** { *; }
-keep class dev.forcetower.breaker.model.** { *; }
-keep class dev.forcetower.breaker.dto.** { *; }
-keep class com.forcetower.uefs.core.storage.database.aggregation.** { *; }

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

-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

## Retrofit will probably break this next update, omegalul
# R8 full mode strips generic signatures from return types if not kept.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

## This was needed because of the CallAdapter. I dont use it anymore, so...
# -keep,allowshrinking class androidx.lifecycle.LiveData

-keep class com.caverock.androidsvg.** { *; }
-dontwarn com.caverock.androidsvg.**
-dontwarn pl.droidsonroids.gif.GifDrawable

-dontwarn okhttp3.internal.platform.ConscryptPlatform
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn android.content.pm.PackageManager$PackageInfoFlags
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite
