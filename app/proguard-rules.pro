# Proguard rules for PRISMA Mobile
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.prisma.mobile.data.model.** { *; }
-dontwarn okio.**
-dontwarn retrofit2.**
