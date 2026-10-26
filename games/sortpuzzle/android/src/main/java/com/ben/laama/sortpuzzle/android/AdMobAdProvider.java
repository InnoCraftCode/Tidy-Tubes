package com.ben.laama.sortpuzzle.android;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration;
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import com.ben.laama.sortpuzzle.ads.AdProvider;
import com.ben.laama.sortpuzzle.ads.RewardGate;

/**
 * AdMob via the GMA Next-Gen SDK, gated by UMP consent. The only class in the game that
 * imports an ad SDK.
 *
 * <p>Flow: request consent info on every launch (UMP shows a form only where required) → start
 * the SDK once {@code canRequestAds()} is true → keep one rewarded and one interstitial loaded,
 * retrying failures with exponential backoff. Threads: SDK callbacks may arrive on any thread,
 * ads are shown on the UI thread, and every callback into the game is posted to the render
 * thread. Rewards settle exactly once through {@link RewardGate}.</p>
 *
 * <p>Debug builds receive Google's demo unit IDs from Gradle and test devices from
 * {@code admob.properties}; never test with production IDs on your own device.</p>
 */
public final class AdMobAdProvider implements AdProvider {

    private static final String TAG = "AdMobAdProvider";
    private static final long MAX_RETRY_DELAY_MS = 64_000L;

    /** Everything that varies per build, filled from BuildConfig by the launcher. */
    public static final class Config {
        final String appId;
        final String rewardedUnitId;
        final String interstitialUnitId;
        final List<String> testDeviceIds;
        final boolean debug;

        public Config(String appId, String rewardedUnitId, String interstitialUnitId,
                      String commaSeparatedTestDeviceIds, boolean debug) {
            this.appId = appId;
            this.rewardedUnitId = rewardedUnitId;
            this.interstitialUnitId = interstitialUnitId;
            this.debug = debug;
            this.testDeviceIds = new ArrayList<>();
            for (String id : commaSeparatedTestDeviceIds.split(",")) {
                if (!id.trim().isEmpty()) {
                    testDeviceIds.add(id.trim());
                }
            }
        }
    }

    private final Activity activity;
    private final Config config;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean sdkStarted = new AtomicBoolean();
    /** Written on the UI thread, read from the render thread (settings screen). */
    private volatile ConsentInformation consent;

    private volatile RewardedAd rewarded;
    private volatile InterstitialAd interstitial;
    private final AtomicBoolean loadingRewarded = new AtomicBoolean();
    private final AtomicBoolean loadingInterstitial = new AtomicBoolean();
    private volatile int rewardedRetries;
    private volatile int interstitialRetries;

    public AdMobAdProvider(Activity activity, Config config) {
        this.activity = activity;
        this.config = config;
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void initialize() {
        activity.runOnUiThread(this::gatherConsent);
    }

    private void gatherConsent() {
        consent = UserMessagingPlatform.getConsentInformation(activity);
        ConsentRequestParameters.Builder params = new ConsentRequestParameters.Builder();
        if (config.debug && !config.testDeviceIds.isEmpty()) {
            ConsentDebugSettings.Builder debug = new ConsentDebugSettings.Builder(activity);
            for (String id : config.testDeviceIds) {
                debug.addTestDeviceHashedId(id);
            }
            params.setConsentDebugSettings(debug.build());
        }
        consent.requestConsentInfoUpdate(activity, params.build(),
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                    if (formError != null) {
                        Log.w(TAG, "Consent form: " + formError.getMessage());
                    }
                    startSdkIfAllowed();
                }),
                requestError -> {
                    Log.w(TAG, "Consent info update failed: " + requestError.getMessage());
                    startSdkIfAllowed(); // falls back to the previous session's consent
                });
        // Consent from a previous session may already allow requests; don't wait for the network.
        startSdkIfAllowed();
    }

    private void startSdkIfAllowed() {
        if (consent == null || !consent.canRequestAds() || !sdkStarted.compareAndSet(false, true)) {
            return;
        }
        InitializationConfig.Builder init = new InitializationConfig.Builder(config.appId);
        if (config.debug && !config.testDeviceIds.isEmpty()) {
            init.setRequestConfiguration(new RequestConfiguration.Builder()
                    .setTestDeviceIds(config.testDeviceIds)
                    .build());
        }
        // Initialising on the main thread can cause an ANR (Google's guidance).
        new Thread(() -> MobileAds.initialize(activity.getApplicationContext(), init.build(), status -> {
            Log.i(TAG, "GMA SDK initialised");
            loadRewarded();
            loadInterstitial();
        }), "ads-init").start();
    }

    @Override
    public void dispose() {
        main.removeCallbacksAndMessages(null);
        RewardedAd r = rewarded;
        InterstitialAd i = interstitial;
        rewarded = null;
        interstitial = null;
        if (r != null) r.destroy();
        if (i != null) i.destroy();
    }

    // ------------------------------------------------------------------ rewarded

    private void loadRewarded() {
        if (!sdkStarted.get() || rewarded != null || !loadingRewarded.compareAndSet(false, true)) {
            return;
        }
        RewardedAd.load(new AdRequest.Builder(config.rewardedUnitId).build(), new AdLoadCallback<RewardedAd>() {
            @Override
            public void onAdLoaded(RewardedAd ad) {
                rewarded = ad;
                rewardedRetries = 0;
                loadingRewarded.set(false);
                Log.i(TAG, "Rewarded loaded");
            }

            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                loadingRewarded.set(false);
                // No fill is normal; back off instead of hammering the network.
                main.postDelayed(AdMobAdProvider.this::loadRewarded, backoff(++rewardedRetries));
                Log.i(TAG, "Rewarded failed to load: " + error.getMessage());
            }
        });
    }

    @Override
    public boolean isRewardedReady() {
        return rewarded != null;
    }

    @Override
    public void showRewarded(RewardListener listener) {
        RewardGate gate = new RewardGate(new RewardListener() {
            @Override
            public void onRewarded(int coins) {
                Gdx.app.postRunnable(() -> listener.onRewarded(coins));
            }

            @Override
            public void onFailed() {
                Gdx.app.postRunnable(listener::onFailed);
            }
        });
        RewardedAd ad = rewarded;
        if (ad == null) {
            gate.onFailed();
            loadRewarded();
            return;
        }
        rewarded = null; // an ad object is single-use
        ad.setImmersiveMode(true);
        ad.setAdEventCallback(new RewardedAdEventCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                gate.onDismissed();
                ad.destroy();
                loadRewarded();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(FullScreenContentError error) {
                Log.w(TAG, "Rewarded failed to show: " + error.getMessage());
                gate.onFailed();
                ad.destroy();
                loadRewarded();
            }
        });
        // The game decides what the reward is (hints, moves); the network's amount is passed
        // through for completeness only.
        activity.runOnUiThread(() -> ad.show(activity, rewardItem -> gate.markEarned(rewardItem.getAmount())));
    }

    // ------------------------------------------------------------------ interstitial

    private void loadInterstitial() {
        if (!sdkStarted.get() || interstitial != null || !loadingInterstitial.compareAndSet(false, true)) {
            return;
        }
        InterstitialAd.load(new AdRequest.Builder(config.interstitialUnitId).build(), new AdLoadCallback<InterstitialAd>() {
            @Override
            public void onAdLoaded(InterstitialAd ad) {
                interstitial = ad;
                interstitialRetries = 0;
                loadingInterstitial.set(false);
                Log.i(TAG, "Interstitial loaded");
            }

            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                loadingInterstitial.set(false);
                main.postDelayed(AdMobAdProvider.this::loadInterstitial, backoff(++interstitialRetries));
                Log.i(TAG, "Interstitial failed to load: " + error.getMessage());
            }
        });
    }

    @Override
    public boolean isInterstitialReady() {
        return interstitial != null;
    }

    @Override
    public void showInterstitial(Runnable onClosed) {
        InterstitialAd ad = interstitial;
        if (ad == null) {
            Gdx.app.postRunnable(onClosed);
            loadInterstitial();
            return;
        }
        interstitial = null;
        AtomicBoolean closed = new AtomicBoolean();
        Runnable closeOnce = () -> {
            if (closed.compareAndSet(false, true)) {
                Gdx.app.postRunnable(onClosed);
                ad.destroy();
                loadInterstitial();
            }
        };
        ad.setImmersiveMode(true);
        ad.setAdEventCallback(new InterstitialAdEventCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                closeOnce.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(FullScreenContentError error) {
                Log.w(TAG, "Interstitial failed to show: " + error.getMessage());
                closeOnce.run();
            }
        });
        activity.runOnUiThread(() -> ad.show(activity));
    }

    // ------------------------------------------------------------------ privacy

    @Override
    public boolean isPrivacyOptionsRequired() {
        ConsentInformation c = consent;
        return c != null && c.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    @Override
    public void showPrivacyOptions() {
        activity.runOnUiThread(() -> UserMessagingPlatform.showPrivacyOptionsForm(activity, formError -> {
            if (formError != null) {
                Log.w(TAG, "Privacy options form: " + formError.getMessage());
            }
            startSdkIfAllowed();
        }));
    }

    private static long backoff(int attempt) {
        return Math.min(MAX_RETRY_DELAY_MS, 1000L << Math.min(attempt, 6));
    }
}
