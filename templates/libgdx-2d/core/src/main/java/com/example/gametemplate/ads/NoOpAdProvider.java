package com.example.gametemplate.ads;

import com.badlogic.gdx.Gdx;

/**
 * Used on desktop, in tests, and in any build with {@code ads.enabled=false}.
 *
 * <p><b>Grants every reward immediately</b> so reward-gated flows stay testable without an ad SDK.
 * The log line is the reminder that nobody is being shown anything. Never shows interstitials.</p>
 */
public final class NoOpAdProvider implements AdProvider {

    private static final String TAG = "NoOpAdProvider";

    @Override
    public void initialize() {
        // nothing to set up
    }

    @Override
    public boolean isInterstitialReady() {
        return false;
    }

    @Override
    public void showInterstitial(Runnable onClosed) {
        onClosed.run();
    }

    @Override
    public boolean isRewardedReady() {
        return true;
    }

    @Override
    public void showRewarded(RewardListener listener) {
        if (Gdx.app != null) {
            Gdx.app.log(TAG, "Ads disabled - granting the reward without showing a video.");
        }
        listener.onRewarded();
    }

    @Override
    public boolean isPrivacyOptionsRequired() {
        return false;
    }

    @Override
    public void showPrivacyOptions() {
        // nothing to show
    }

    @Override
    public void dispose() {
        // nothing to release
    }
}
