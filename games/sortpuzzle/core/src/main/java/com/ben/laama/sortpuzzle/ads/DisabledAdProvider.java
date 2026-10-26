package com.ben.laama.sortpuzzle.ads;

/**
 * The provider for builds without ads: {@code -Pads.enabled=false}, a release build with no
 * {@code admob.properties}, or {@link AdsConfig#ADS_ENABLED} off.
 *
 * <p>It never reports an ad as ready, never grants a reward and never touches the network or
 * any SDK. The screens check {@link #isRewardedReady()} first and offer the coin-based path
 * instead, so in practice {@link #showRewarded} is not even reached; if it is, it fails
 * immediately and honestly. Pure Java, so it is covered by a unit test.</p>
 */
public final class DisabledAdProvider implements AdProvider {

    private int showAttempts;

    @Override
    public void initialize() {
        // Nothing to initialise: there is no SDK behind this provider.
    }

    @Override
    public boolean isInterstitialReady() {
        return false;
    }

    @Override
    public void showInterstitial(Runnable onClosed) {
        showAttempts++;
        onClosed.run();
    }

    @Override
    public boolean isRewardedReady() {
        return false;
    }

    @Override
    public void showRewarded(RewardListener listener) {
        showAttempts++;
        listener.onFailed();
    }

    @Override
    public boolean isPrivacyOptionsRequired() {
        return false;
    }

    @Override
    public void showPrivacyOptions() {
        // Nothing to show.
    }

    @Override
    public void dispose() {
        // Nothing to release.
    }

    /** @return how many times a screen asked this provider to show something (test hook) */
    public int showAttempts() {
        return showAttempts;
    }
}
