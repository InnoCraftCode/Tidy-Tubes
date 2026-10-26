package com.example.gametemplate.ads;

/**
 * Platform-agnostic advertising hook. Core code talks only to this interface; each launcher
 * injects an implementation (NoOp on desktop and in ad-free builds, AdMob on Android).
 *
 * <p>Contract for implementations:</p>
 * <ul>
 *   <li>Every callback is delivered on the libGDX render thread, exactly once.</li>
 *   <li>{@code show*} never blocks and is safe to call when nothing is loaded — the callback then
 *       fires immediately with the "nothing shown" outcome.</li>
 *   <li>A reward is granted only after the network confirms it was earned.</li>
 * </ul>
 */
public interface AdProvider {

    /** Called once from {@code create()}. Must not block. */
    void initialize();

    boolean isInterstitialReady();

    /** @param onClosed runs after the ad is dismissed, or immediately when none could be shown. */
    void showInterstitial(Runnable onClosed);

    boolean isRewardedReady();

    void showRewarded(RewardListener listener);

    /**
     * Whether the game must show a "Privacy options" button (UMP says so for users in regulated
     * regions). The button calls {@link #showPrivacyOptions()}.
     */
    boolean isPrivacyOptionsRequired();

    void showPrivacyOptions();

    void dispose();

    /** Outcome of a rewarded ad. Exactly one method is called per {@code showRewarded}. */
    interface RewardListener {
        void onRewarded();

        /** No fill, failed to show, or dismissed early. No reward is owed. */
        void onFailed();
    }
}
