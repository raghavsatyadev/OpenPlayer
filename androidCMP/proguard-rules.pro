
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keepattributes Signature, InnerClasses, EnclosingMethod, AnnotationDefault, *Annotation*

-keep class com.kyant.taglib.** { *; }

-keep class org.jaudiotagger.tag.** { *; }
-dontwarn org.jaudiotagger.**

-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.sound.sampled.**
-dontwarn javax.swing.filechooser.FileFilter
-dontwarn javax.lang.model.**

-keep class androidx.media3.decoder.ffmpeg.** { *; }
-keep class androidx.media3.exoplayer.ffmpeg.** { *; }

-keep class androidx.media3.decoder.midi.** { *; }
-keep class com.jsyn.** { *; }
-keep class com.softsynth.** { *; }
-dontwarn com.jsyn.**
-dontwarn com.softsynth.**

-keepclassmembers class io.github.raghavsatyadev.openplayer.data.model.** { *; }

-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

-keep class io.github.raghavsatyadev.openplayer.data.preferences.PreferenceBackupEntry { *; }
-keep class io.github.raghavsatyadev.openplayer.data.backup.model.** { *; }
-keep class io.github.raghavsatyadev.openplayer.data.backup.module.** { *; }
-keep class io.github.raghavsatyadev.openplayer.data.database.FavoritesEntity { *; }
-keep class io.github.raghavsatyadev.openplayer.data.database.SongEngagementEntity { *; }
-keep class io.github.raghavsatyadev.openplayer.data.database.LyricsEntity { *; }
-keep class io.github.raghavsatyadev.openplayer.data.database.SearchHistoryEntity { *; }
-keep class io.github.raghavsatyadev.openplayer.data.database.TransitionRuleEntity { *; }
-keep class io.github.raghavsatyadev.openplayer.data.database.AudioBookmarkEntity { *; }

-keep class io.ktor.server.engine.** { *; }
-keep class io.ktor.server.cio.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn reactor.blockhound.**

-keep class com.atilika.kuromoji.** { *; }
-keepnames class com.atilika.kuromoji.** { *; }
-dontwarn com.atilika.kuromoji.**

-keep class net.sourceforge.pinyin4j.** { *; }
-keepclassmembers class net.sourceforge.pinyin4j.** { *; }
-dontwarn net.sourceforge.pinyin4j.**

-keep class * extends androidx.glance.appwidget.action.ActionCallback { <init>(); }

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
