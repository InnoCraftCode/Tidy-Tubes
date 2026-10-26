package com.example.gametemplate.android;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import com.example.gametemplate.BuildConfig;
import com.example.gametemplate.TemplateGame;
import com.example.gametemplate.ads.AdProvider;
import com.example.gametemplate.ads.NoOpAdProvider;

/**
 * Android entry point. The only platform decision made here is which {@link AdProvider} the game
 * gets; ad IDs and the on/off switch come from Gradle (see android/build.gradle.kts).
 */
public final class AndroidLauncher extends AndroidApplication {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;
        // Touch-only game: sensors off saves battery.
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useGyroscope = false;
        initialize(new TemplateGame(createAds()), config);
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
