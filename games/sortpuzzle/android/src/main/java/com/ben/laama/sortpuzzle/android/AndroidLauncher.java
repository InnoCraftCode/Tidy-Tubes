package com.ben.laama.sortpuzzle.android;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import com.ben.laama.sortpuzzle.BuildConfig;
import com.ben.laama.sortpuzzle.Main;
import com.ben.laama.sortpuzzle.ads.AdProvider;
import com.ben.laama.sortpuzzle.ads.AdProviders;
import com.ben.laama.sortpuzzle.services.GameServices;

/**
 * Android entry point.
 *
 * <p>Platform decisions made here: which {@link AdProvider} the game gets, how the system bars
 * are hidden, and when the orientation is locked. Ad IDs and the on/off switch come from Gradle
 * ({@code android/build.gradle.kts}): demo IDs in debug, {@code admob.properties} in release,
 * {@code -Pads.enabled=false} to turn ads off. A build without ads gets the inert provider from
 * {@link AdProviders#forAndroid}.</p>
 *
 * <p>Edge to edge: the window draws under the display cutout through the theme
 * ({@code values-v30/styles.xml}, {@code windowLayoutInDisplayCutoutMode=always}, the only value
 * Android 15 still honours) and the screens step their UI aside by
 * {@code Gdx.graphics.getSafeInset*}. System bars are hidden with
 * {@link WindowInsetsControllerCompat}; the libGDX equivalents use APIs Android 15 deprecated,
 * which Play Console reports, so both of its switches stay off.</p>
 *
 * <p>The manifest declares no {@code screenOrientation}: Google Play flags fixed orientations as
 * large-screen restrictions and Android 16 ignores them above 600 dp anyway. Phones are kept in
 * portrait at runtime instead; tablets, foldables and desktop windows rotate freely and the
 * {@code ExtendViewport} fills whatever shape they take.</p>
 */
public final class AndroidLauncher extends AndroidApplication {

    /** Windows narrower than this (smallest width, dp) are phone-sized and stay in portrait. */
    private static final int COMPACT_WIDTH_DP = 600;

    private AndroidReminders reminders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        // Both handled here instead (see the class comment): libGDX would call the deprecated
        // View.setSystemUiVisibility and set LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES.
        config.useImmersiveMode = false;
        config.renderUnderCutout = false;
        // The game reads nothing but touches: leaving the sensors off saves battery.
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useGyroscope = false;
        // Cheap multisampling; the procedurally generated balls are already anti-aliased, this
        // just tidies the tube edges.
        config.numSamples = 2;

        // Optional services, each inert unless its config file was present at build time:
        // Play Games (playgames.properties) and Firebase (google-services.json).
        GameServices services = createGameServices();
        FirebaseAnalyticsBridge analytics = BuildConfig.FIREBASE_CONFIGURED
            ? FirebaseAnalyticsBridge.create(this) : null;
        AdProvider network = createNetwork();
        if (analytics != null && network instanceof AdMobAdProvider) {
            // Analytics and crash reports follow the ads consent decision; never in debug builds.
            ((AdMobAdProvider) network).setConsentListener(canRequestAds ->
                analytics.setCollectionEnabled(canRequestAds && !BuildConfig.DEBUG));
        }
        reminders = new AndroidReminders(this);
        reminders.handleLaunchIntent(getIntent()); // cold start from a tapped reminder
        initialize(new Main(AdProviders.forAndroid(network), services, analytics, reminders,
            new AndroidShare(this)), config);
        hideSystemBars();
        applyOrientationPolicy(getResources().getConfiguration());
    }

    /** singleTask: a reminder tapped while the game is alive arrives here, then onResume runs. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (reminders != null) {
            reminders.handleLaunchIntent(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AndroidReminders.PERMISSION_REQUEST && reminders != null) {
            reminders.onPermissionResult(grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        }
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

    /**
     * Sticky immersive mode: bars hidden, a swipe from the edge shows them for a moment. The
     * compat controller uses WindowInsetsController on Android 11+ and the legacy flags below.
     */
    private void hideSystemBars() {
        WindowInsetsControllerCompat controller =
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setSystemBarsBehavior(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.systemBars());
    }

    /**
     * Portrait on phone-sized windows, free rotation everywhere else. Decided from the smallest
     * window width, the signal Google recommends, so it cannot flicker between the two.
     */
    private void applyOrientationPolicy(Configuration configuration) {
        boolean compact = configuration.smallestScreenWidthDp < COMPACT_WIDTH_DP;
        int wanted = compact
            ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        if (getRequestedOrientation() != wanted) {
            setRequestedOrientation(wanted);
        }
    }

    /** @return Play Games Services, or {@code null} when this build has no playgames.properties */
    private GameServices createGameServices() {
        if (!BuildConfig.PLAY_GAMES_ENABLED) {
            return null;
        }
        AndroidGameServices.initializeSdk(this);
        return new AndroidGameServices(this, new AndroidGameServices.Config(
            BuildConfig.LEADERBOARD_HIGHEST_LEVEL,
            BuildConfig.LEADERBOARD_TOTAL_STARS,
            BuildConfig.LEADERBOARD_DAILY_MOVES));
    }

    /** @return the AdMob provider, or {@code null} when this build carries no ads */
    private AdProvider createNetwork() {
        if (!BuildConfig.ADS_ENABLED) {
            return null;
        }
        return new AdMobAdProvider(this, new AdMobAdProvider.Config(
                BuildConfig.ADMOB_APP_ID,
                BuildConfig.AD_UNIT_REWARDED,
                BuildConfig.AD_UNIT_INTERSTITIAL,
                BuildConfig.TEST_DEVICE_IDS,
                BuildConfig.DEBUG));
    }
}
