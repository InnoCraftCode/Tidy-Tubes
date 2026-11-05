package com.example.gametemplate.android;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import com.example.gametemplate.BuildConfig;
import com.example.gametemplate.TemplateGame;
import com.example.gametemplate.ads.AdProvider;
import com.example.gametemplate.ads.NoOpAdProvider;

/**
 * Android entry point. Platform decisions made here: which {@link AdProvider} the game gets (ad
 * IDs and the on/off switch come from Gradle, see android/build.gradle.kts), how the system bars
 * are hidden, and when the orientation is locked.
 *
 * <p>Edge to edge: the window draws under the display cutout through the theme
 * ({@code values-v30/styles.xml}, {@code windowLayoutInDisplayCutoutMode=always}, the only value
 * Android 15 still honours); pad the UI with {@code Gdx.graphics.getSafeInset*}. System bars are
 * hidden with {@link WindowInsetsControllerCompat}; the libGDX equivalents use APIs Android 15
 * deprecated, which Play Console reports, so both of its switches stay off.</p>
 *
 * <p>The manifest declares no {@code screenOrientation}: Google Play flags fixed orientations as
 * large-screen restrictions and Android 16 ignores them above 600 dp anyway. Phones are kept in
 * portrait at runtime instead; tablets, foldables and desktop windows rotate freely, so screens
 * must lay out from the stage size (ExtendViewport + fill-parent tables).</p>
 */
public final class AndroidLauncher extends AndroidApplication {

    /** Windows narrower than this (smallest width, dp) are phone-sized and stay in portrait. */
    private static final int COMPACT_WIDTH_DP = 600;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        // Both handled here instead (see the class comment): libGDX would call the deprecated
        // View.setSystemUiVisibility and set LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES.
        config.useImmersiveMode = false;
        config.renderUnderCutout = false;
        // Touch-only game: sensors off saves battery.
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useGyroscope = false;
        initialize(new TemplateGame(createAds()), config);
        hideSystemBars();
        applyOrientationPolicy(getResources().getConfiguration());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Coming back from an ad, a dialog or the recents screen restores the bars.
            hideSystemBars();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Folding, unfolding or resizing a desktop window changes the smallest width.
        applyOrientationPolicy(newConfig);
    }

    /** Sticky immersive mode: bars hidden, a swipe from the edge shows them for a moment. */
    private void hideSystemBars() {
        WindowInsetsControllerCompat controller =
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setSystemBarsBehavior(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.systemBars());
    }

    /** Portrait on phone-sized windows, free rotation everywhere else. */
    private void applyOrientationPolicy(Configuration configuration) {
        boolean compact = configuration.smallestScreenWidthDp < COMPACT_WIDTH_DP;
        int wanted = compact
            ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        if (getRequestedOrientation() != wanted) {
            setRequestedOrientation(wanted);
        }
    }

    private AdProvider createAds() {
        if (!BuildConfig.ADS_ENABLED) {
            return new NoOpAdProvider();
        }
        return new AdMobAdProvider(this, new AdMobAdProvider.Config(
                BuildConfig.ADMOB_APP_ID,
                BuildConfig.AD_UNIT_REWARDED,
                BuildConfig.AD_UNIT_INTERSTITIAL,
                BuildConfig.TEST_DEVICE_IDS,
                BuildConfig.DEBUG));
    }
}
