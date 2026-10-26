package com.ben.laama.sortpuzzle.ads;

/**
 * Chooses the {@link AdProvider} for a build.
 *
 * <p>Every launcher goes through here, so the decision lives in one place and follows
 * {@link AdsConfig#ADS_ENABLED}. On a device a missing real network always means
 * {@link DisabledAdProvider} — never the development stub, which would hand out rewards for
 * nothing. The stub ({@link NoOpAdProvider}) is for the desktop, where there is no ad SDK and
 * the reward flows still need exercising.</p>
 */
public final class AdProviders {

    private AdProviders() {
    }

    /** @return the provider for desktop runs, tests and captures */
    public static AdProvider forDesktop() {
        return AdsConfig.ADS_ENABLED ? new NoOpAdProvider() : new DisabledAdProvider();
    }

    /**
     * @param realNetwork the platform's real implementation, or {@code null} when this build
     *                    has no ads (demo/production IDs absent, or {@code -Pads.enabled=false})
     * @return the provider the Android launcher should hand to {@code Main}
     */
    public static AdProvider forAndroid(AdProvider realNetwork) {
        if (!AdsConfig.ADS_ENABLED || realNetwork == null) {
            return new DisabledAdProvider();
        }
        return realNetwork;
    }
}
