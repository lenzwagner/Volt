-keepattributes *Annotation*
-keepclassmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class com.lenz.tennisapp.data.api.dto.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
