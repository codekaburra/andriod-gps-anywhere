# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK tools.

# Keep Room entities
-keep class com.gpsanywhere.app.data.** { *; }

# Gson: TypeToken needs generic signatures, and stored route JSON uses these field names.
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.gpsanywhere.app.routes.LocationPoint { *; }
