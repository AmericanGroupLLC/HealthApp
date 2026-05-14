# ─── Room (SQLite ORM) ──────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class androidx.room.** { *; }

# ─── Hilt (Dependency Injection) ────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keepclassmembers class * {
    @dagger.hilt.android.qualifiers.ApplicationContext *;
}
-dontwarn dagger.hilt.**

# ─── Ktor / Kotlinx Serialization ──────────────────────────────────────
-keep class io.ktor.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn io.ktor.**

# ─── Health Connect ─────────────────────────────────────────────────────
-keep class androidx.health.connect.client.** { *; }
-keep class androidx.health.** { *; }

# ─── SQLCipher ──────────────────────────────────────────────────────────
-keep class net.zetetic.database.** { *; }
-keep class net.sqlcipher.** { *; }
-keepclassmembers class net.sqlcipher.** { *; }
-keep,includedescriptorclasses class net.sqlcipher.database.** { native <methods>; }

# ─── General Android ───────────────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
