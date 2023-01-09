# Copyright 2016 The Chromium Authors
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

# Contains flags that can be safely shared with Cronet, and thus would be
# appropriate for third-party apps to include.

-keep @androidx.annotation.Keep class *
-keepclasseswithmembers,allowaccessmodification class * {
  @androidx.annotation.Keep <fields>;
}
-keepclasseswithmembers,allowaccessmodification class * {
  @androidx.annotation.Keep <methods>;
}

# Even unused methods kept due to explicit jni registration:
# https://crbug.com/688465.
-keepclasseswithmembers,includedescriptorclasses,allowaccessmodification class !org.chromium.base.library_loader.**,** {
  native <methods>;
}
-keepclasseswithmembernames,includedescriptorclasses,allowaccessmodification class org.chromium.base.library_loader.** {
  native <methods>;
}

# Use assumevalues block instead of assumenosideeffects block because Google3 proguard cannot parse
# assumenosideeffects blocks which overwrite return value.
# chromium_code.flags rather than remove_logging.flags so that it's included
# in cronet.
-assumevalues class org.chromium.base.Log {
  static boolean isDebug() return false;
}

# Keep all CREATOR fields within Parcelable that are kept.
-keepclassmembers class org.chromium.** implements android.os.Parcelable {
  public static *** CREATOR;
}

# Don't obfuscate Parcelables as they might be marshalled outside Chrome.
# If we annotated all Parcelables that get put into Bundles other than
# for saveInstanceState (e.g. PendingIntents), then we could actually keep the
# names of just those ones. For now, we'll just keep them all.
-keepnames,allowaccessmodification class org.chromium.** implements android.os.Parcelable {}

# Keep all enum values and valueOf methods. See
# http://proguard.sourceforge.net/index.html#manual/examples.html
# for the reason for this. Also, see http://crbug.com/248037.
-keepclassmembers enum org.chromium.** {
    public static **[] values();
}

# -identifiernamestring doesn't keep the module impl around, we have to
# explicitly keep it.
-if @org.chromium.components.module_installer.builder.ModuleInterface interface *
-keep,allowobfuscation,allowaccessmodification class * extends <1> {
  <init>();
}
# Copyright 2022 The Chromium Authors
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

# Contains flags related to annotations in //build/android that can be safely
# shared with Cronet, and thus would be appropriate for third-party apps to
# include.

# Keep all annotation related attributes that can affect runtime
-keepattributes RuntimeVisible*Annotations
-keepattributes AnnotationDefault

# Keep the annotations, because if we don't, the ProGuard rules that use them
# will not be respected. These classes then show up in our final dex, which we
# do not want - see crbug.com/628226.
-keep @interface org.chromium.base.annotations.AccessedByNative
-keep @interface org.chromium.base.annotations.CalledByNative
-keep @interface org.chromium.base.annotations.CalledByNativeUnchecked
-keep @interface org.chromium.build.annotations.DoNotInline
-keep @interface org.chromium.build.annotations.UsedByReflection
-keep @interface org.chromium.build.annotations.IdentifierNameString

# Keeps for class level annotations.
-keep,allowaccessmodification @org.chromium.build.annotations.UsedByReflection class ** {}

# Keeps for method level annotations.
-keepclasseswithmembers,allowaccessmodification class ** {
  @org.chromium.base.annotations.AccessedByNative <fields>;
}
-keepclasseswithmembers,includedescriptorclasses,allowaccessmodification class ** {
  @org.chromium.base.annotations.CalledByNative <methods>;
}
-keepclasseswithmembers,includedescriptorclasses,allowaccessmodification class ** {
  @org.chromium.base.annotations.CalledByNativeUnchecked <methods>;
}
-keepclasseswithmembers,allowaccessmodification class ** {
  @org.chromium.build.annotations.UsedByReflection <methods>;
}
-keepclasseswithmembers,allowaccessmodification class ** {
  @org.chromium.build.annotations.UsedByReflection <fields>;
}

# Never inline classes, methods, or fields with this annotation, but allow
# shrinking and obfuscation.
# Relevant to fields when they are needed to store strong references to objects
# that are held as weak references by native code.
-if @org.chromium.build.annotations.DoNotInline class * {
    *** *(...);
}
-keep,allowobfuscation,allowaccessmodification class <1> {
    *** <2>(...);
}
-keepclassmembers,allowobfuscation,allowaccessmodification class * {
   @org.chromium.build.annotations.DoNotInline <methods>;
}
-keepclassmembers,allowobfuscation,allowaccessmodification class * {
   @org.chromium.build.annotations.DoNotInline <fields>;
}

-alwaysinline class * {
    @org.chromium.build.annotations.AlwaysInline *;
}

# Never merge classes horizontally or vertically with this annotation.
# Relevant to classes being used as a key in maps or sets.
-keep,allowaccessmodification,allowobfuscation,allowshrinking @org.chromium.build.annotations.DoNotClassMerge class *

# Mark members annotated with IdentifierNameString as identifier name strings
-identifiernamestring class * {
    @org.chromium.build.annotations.IdentifierNameString *;
}
# Proguard config for apps that depend on cronet_impl_native_java.jar.

# This constructor is called using the reflection from Cronet API (cronet_api.jar).
-keep class * extends org.chromium.net.CronetProvider {
    public <init>(android.content.Context);
}

# Suppress unnecessary warnings.
-dontnote org.chromium.net.ProxyChangeListener$ProxyReceiver
-dontnote org.chromium.net.AndroidKeyStore
# Needs 'void setTextAppearance(int)' (API level 23).
-dontwarn org.chromium.base.ApiCompatibilityUtils
# Needs 'boolean onSearchRequested(android.view.SearchEvent)' (API level 23).
-dontwarn org.chromium.base.WindowCallbackWrapper

# Generated for chrome apk and not included into cronet.
-dontwarn org.chromium.base.multidex.ChromiumMultiDexInstaller
-dontwarn org.chromium.base.library_loader.LibraryLoader
-dontwarn org.chromium.base.SysUtils
-dontwarn org.chromium.build.NativeLibraries

# Objects of this type are passed around by native code, but the class
# is never used directly by native code. Since the class is not loaded, it does
# not need to be preserved as an entry point.
-dontnote org.chromium.net.UrlRequest$ResponseHeadersMap
# https://android.googlesource.com/platform/sdk/+/marshmallow-mr1-release/files/proguard-android.txt#54
-dontwarn android.support.**

# This class should be explicitly kept to avoid failure if
# class/merging/horizontal proguard optimization is enabled.
-keep class org.chromium.base.CollectionUtil

