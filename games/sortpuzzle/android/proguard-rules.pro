# ---------------------------------------------------------------------------------------------
# libGDX
# ---------------------------------------------------------------------------------------------
-keep class com.badlogic.gdx.backends.android.AndroidApplication { *; }
-keep class com.badlogic.gdx.backends.android.AndroidInput { *; }
-keep class com.badlogic.gdx.controllers.android.AndroidControllers { *; }

-keepclassmembers class com.badlogic.gdx.backends.android.AndroidInput* {
   <init>(...);
}

# The engine reaches for these reflectively depending on the backend; without the -dontwarn the
# release build fails on classes that are simply not part of an Android target.
-dontwarn android.support.**
-dontwarn com.badlogic.gdx.jnigen.**
-dontwarn com.badlogic.gdx.utils.GdxBuild
-dontwarn com.badlogic.gdx.physics.box2d.utils.Box2DBuild
-dontwarn com.badlogic.gdx.graphics.g2d.freetype.FreetypeBuild
-dontwarn com.badlogic.gdx.backends.lwjgl3.**
-dontwarn org.lwjgl.**

# scene2d actions and events are pooled: Pools/ReflectionPool instantiate them reflectively
# through their no-arg constructor, which R8 otherwise strips as unused. Without this rule the
# release build dies on the first animation with "Class cannot be created (missing no-arg
# constructor)" - found on the 2026-10-26 release test, invisible in debug.
# Pooled types live across the engine (scene2d actions/events, GlyphLayout and its runs, ...),
# so the no-arg constructors of every libGDX class are kept. Class names may still be renamed:
# the pools hold Class objects, not strings.
-keepclassmembers class com.badlogic.gdx.** {
    public <init>();
}
-keepclassmembers class * extends com.badlogic.gdx.scenes.scene2d.Action {
    public <init>();
}
-keepclassmembers class * extends com.badlogic.gdx.scenes.scene2d.Event {
    public <init>();
}

# ---------------------------------------------------------------------------------------------
# Our own reflective surface
# ---------------------------------------------------------------------------------------------
# SaveData is written and read by libGDX Json, which walks its fields reflectively. Renaming or
# stripping any of them silently wipes the player's progress on release builds only — the kind
# of bug that never shows up in debug.
-keep class com.ben.laama.sortpuzzle.save.SaveData { *; }
-keepclassmembers class com.ben.laama.sortpuzzle.save.SaveData {
    <fields>;
    <init>(...);
}

# The GMA Next-Gen and UMP SDKs ship their own consumer ProGuard rules; nothing to add for them.

# Enum values are looked up by name in a few places.
-keepclassmembers enum com.ben.laama.sortpuzzle.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
