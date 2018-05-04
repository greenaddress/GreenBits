# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ANDROID_HOME/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-mergeinterfacesaggressively

-keep class org.codehaus.jackson.** { *; }
-keep public class android.support.v7.widget.** { *; }
-keep public class android.support.v7.internal.widget.** { *; }
-keep public class android.support.v7.internal.view.menu.** { *; }

-keep public class * extends android.support.v4.view.ActionProvider {
    public <init>(android.content.Context);
}

# BEGIN for protobuf in trezor:
-keep class com.satoshilabs.trezor.** { *; }
-keepattributes InnerClasses,EnclosingMethod
# END for protobuf in trezor

-dontwarn org.codehaus.jackson.**
-dontwarn com.google.common.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn java.lang.ClassValue
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn afu.org.checkerframework.**
-dontwarn org.checkerframework.**
-dontwarn org.slf4j.**
-dontwarn sun.nio.**
-dontwarn sun.misc.**
-dontwarn okio.**
-dontwarn org.bitcoinj.store.**
-dontwarn com.mysql.**
-dontwarn org.h2.**
-dontwarn org.postgresql.**
-dontwarn org.jboss.**
-dontwarn org.eclipse.**
-dontwarn io.netty.**
-dontwarn org.apache.**
-dontwarn org.w3c.**
-dontwarn com.ibm.**
-dontwarn com.sun.**
-dontwarn javax.annotation.**
-dontwarn javax.inject.**

-keepattributes Signature,InnerClasses
-keepclasseswithmembers class io.netty.** {*;}
-keepnames class io.netty.** {*;}

-keepnames class ** {*;}
-keepattributes SourceFile,LineNumberTable

-keep class mehdi.sakout.aboutpage.** {*;}
-keep class com.google.common.collect.BiMap {*;}
-keep class com.google.common.collect.ImmutableList {*;}
-keep class com.google.common.collect.ImmutableMap {*;}
-keep class com.google.common.collect.Lists {*;}
-keep class com.google.common.collect.Maps {*;}
-keep class com.blockstream.libwally.** {*;}

-keepattributes InnerClasses,EnclosingMethod
