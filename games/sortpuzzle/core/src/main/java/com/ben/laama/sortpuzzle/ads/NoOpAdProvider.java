package com.ben.laama.sortpuzzle.ads;

import com.badlogic.gdx.Gdx;

/**
 * Development stand-in for the desktop, where there is no ad SDK at all.
 *
 * <p><b>It grants every reward immediately.</b> That is deliberate: the hint system and the
 * out-of-moves flow both hang off {@link #showRewarded}, and a provider that always refused
 * would leave half the game untestable on a desktop. Watch for the log line — it is the
 * reminder that nobody is being shown anything. It is never used on a device:
 * {@link AdProviders#forAndroid} hands out {@link DisabledAdProvider} when no real network
 * exists.</p>
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
        Gdx.app.log(TAG, "No ad network on this platform - granting the reward without showing a video.");
        listener.onRewarded(0);
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
