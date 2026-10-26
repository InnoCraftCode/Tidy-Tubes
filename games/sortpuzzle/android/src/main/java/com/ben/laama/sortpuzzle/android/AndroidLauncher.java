package com.ben.laama.sortpuzzle.android;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import com.ben.laama.sortpuzzle.BuildConfig;
import com.ben.laama.sortpuzzle.Main;
import com.ben.laama.sortpuzzle.ads.AdProvider;
import com.ben.laama.sortpuzzle.ads.AdProviders;

/**
 * Android entry point.
 *
 * <p>The only platform-specific decision made here is which {@link AdProvider} the game gets.
 * Ad IDs and the on/off switch come from Gradle ({@code android/build.gradle.kts}): demo IDs in
 * debug, {@code admob.properties} in release, {@code -Pads.enabled=false} to turn ads off. A
 * build without ads gets the inert provider from {@link AdProviders#forAndroid}.</p>
 */
public final class AndroidLauncher extends AndroidApplication {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;
        // The game reads nothing but touches — leaving the sensors off saves battery.
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useGyroscope = false;
        // Cheap multisampling; the procedurally generated balls are already anti-aliased, this
        // just tidies the tube edges.
        config.numSamples = 2;

        initialize(new Main(AdProviders.forAndroid(createNetwork())), config);
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
