# --- libGDX -----------------------------------------------------------------------------------
-keep class com.badlogic.gdx.backends.android.AndroidApplication { *; }
-keep class com.badlogic.gdx.backends.android.AndroidInput { *; }
-keepclassmembers class com.badlogic.gdx.backends.android.AndroidInput* {
   <init>(...);
}
-dontwarn android.support.**
-dontwarn com.badlogic.gdx.jnigen.**
-dontwarn com.badlogic.gdx.utils.GdxBuild
-dontwarn com.badlogic.gdx.physics.box2d.utils.Box2DBuild
-dontwarn com.badlogic.gdx.graphics.g2d.freetype.FreetypeBuild
-dontwarn com.badlogic.gdx.backends.lwjgl3.**
-dontwarn org.lwjgl.**

# --- Game ---------------------------------------------------------------------------------------
# Anything serialised reflectively (libGDX Json, Preferences of objects) must be kept here, or
# release builds silently lose player data. Add your save classes:
# -keep class com.example.gametemplate.save.** { *; }

# The GMA and UMP SDKs ship their own consumer ProGuard rules; nothing to add for them.
