# SchemaUI Preview App ProGuard Rules
# Add project specific ProGuard rules here.

# Keep kotlinx.serialization classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.landoulsi.schemaui.**$$serializer { *; }
-keepclassmembers class com.landoulsi.schemaui.** {
    *** Companion;
}
-keepclasseswithmembers class com.landoulsi.schemaui.** {
    kotlinx.serialization.KSerializer serializer(...);
}
