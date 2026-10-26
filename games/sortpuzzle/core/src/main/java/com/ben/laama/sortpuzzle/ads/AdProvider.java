package com.ben.laama.sortpuzzle.ads;

/**
 * Platform-agnostic advertising hook.
 *
 * <p>The core game only ever talks to this interface. The desktop build uses
 * {@link NoOpAdProvider}, builds without ads use {@link DisabledAdProvider}, and the Android
 * launcher injects the AdMob implementation ({@code android/.../AdMobAdProvider}). Nothing in
 * {@code core} imports an ad SDK.</p>
 *
 * <p>Contract for implementations:</p>
 * <ul>
 *   <li>Every callback is delivered on the libGDX render thread, exactly once.</li>
 *   <li>{@code show*} never blocks and is safe to call when nothing is loaded — the callback
 *       then fires immediately with the "nothing shown" outcome.</li>
 *   <li>A reward is granted only after the network confirms it was earned
 *       ({@link RewardGate}).</li>
 * </ul>
 */
public interface AdProvider {

    /** Called once from {@code Main.create()}. Must never block. */
    void initialize();

    /** @return {@code true} when {@link #showInterstitial} would actually display something. */
    boolean isInterstitialReady();

    /**
     * Shows a full-screen ad if one is ready.
     *
     * @param onClosed invoked on the render thread once the ad is dismissed, or immediately
     *                 when nothing could be shown. Never {@code null}.
     */
    void showInterstitial(Runnable onClosed);

    /** @return {@code true} when {@link #showRewarded} would actually display something. */
    boolean isRewardedReady();

    /**
     * Shows a rewarded ad.
     *
     * @param listener notified on the render thread with the outcome. Never {@code null}.
     */
    void showRewarded(RewardListener listener);

    /**
     * Whether the game must show a "Privacy options" button (the consent platform says so for
     * users in regulated regions). The button calls {@link #showPrivacyOptions()}.
     */
    boolean isPrivacyOptionsRequired();

    /** Re-opens the consent / privacy options form. No-op when there is nothing to show. */
    void showPrivacyOptions();

    /** Releases any native resources. */
    void dispose();

    /** Outcome of a rewarded ad. Exactly one method is called per {@code showRewarded}. */
    interface RewardListener {
        /**
         * The user watched the whole ad and earned the reward.
         *
         * @param coins coins granted by the network, or {@code 0} when the reward is not coins
         *              at all — a hint or a handful of extra moves, say. The caller knows what
         *              it asked for; this is only the network's payload.
         */
        void onRewarded(int coins);

        /** No ad was available, or the user dismissed it early. No reward is owed. */
        void onFailed();
    }
}
