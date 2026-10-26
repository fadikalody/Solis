# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Prevent R8 from merging or obfuscating DashboardContributor implementations.
# This prevents Jetpack Compose LazyColumn key collisions when using `javaClass.name` as a key.
-keep class * implements com.solis.tracker.core.extension.DashboardContributor { *; }

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
