# Add project specific ProGuard rules here.

# AppWidget & Glance Providers
-keep class * extends android.appwidget.AppWidgetProvider { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * implements androidx.glance.appwidget.action.ActionCallback { *; }
-keep class * extends androidx.glance.state.GlanceStateDefinition { *; }

# Glance & Proto DataStore serialization (used for widget state & RemoteViews layout rendering)
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
-keep class androidx.datastore.preferences.protobuf.** { *; }
-keep class androidx.glance.** { *; }
-keepclassmembers class androidx.glance.** { *; }

# Keep all Glance Widget package contents (widgets, actions, config activity, receivers, utils)
-keep class com.maari.glyphbarcomposer.ui.widget.** { *; }
-keepclassmembers class com.maari.glyphbarcomposer.ui.widget.** { *; }

# Nothing Ketchum SDK (Glyph Hardware Interface)
-keep class com.nothing.ketchum.** { *; }
-keep interface com.nothing.ketchum.** { *; }

# Room Database Entities & DAOs
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class com.maari.glyphbarcomposer.data.** { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep class com.maari.glyphbarcomposer.model.** { *; }
