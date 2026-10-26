package com.ben.laama.sortpuzzle.ads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the provider wiring: this release carries ads, a device without a real network gets the
 * inert provider (never the desktop stub that grants rewards for free), and the inert provider
 * never rewards anyone.
 */
@DisplayName("AdProviders")
class AdProvidersTest {

    @Test
    @DisplayName("this release ships with ads enabled")
    void adsEnabled() {
        assertTrue(AdsConfig.ADS_ENABLED);
    }

    @Test
    @DisplayName("a device build without a real network gets the disabled provider, never the stub")
    void androidWithoutNetwork() {
        assertInstanceOf(DisabledAdProvider.class, AdProviders.forAndroid(null));
    }

    @Test
    @DisplayName("a device build with a real network gets exactly that network")
    void androidWithNetwork() {
        AdProvider real = new DisabledAdProvider();
        assertSame(real, AdProviders.forAndroid(real));
    }

    @Test
    @DisplayName("the desktop gets the development stub so reward flows can be exercised")
    void desktop() {
        assertInstanceOf(NoOpAdProvider.class, AdProviders.forDesktop());
    }

    @Test
    @DisplayName("the disabled provider never has an ad ready and never rewards")
    void disabledProviderNeverRewards() {
        DisabledAdProvider provider = new DisabledAdProvider();
        provider.initialize();
        assertFalse(provider.isInterstitialReady());
        assertFalse(provider.isRewardedReady());
        assertFalse(provider.isPrivacyOptionsRequired());

        boolean[] rewarded = {false};
        boolean[] failed = {false};
        provider.showRewarded(new AdProvider.RewardListener() {
            @Override
            public void onRewarded(int coins) {
                rewarded[0] = true;
            }

            @Override
            public void onFailed() {
                failed[0] = true;
            }
        });
        assertFalse(rewarded[0], "no reward may ever be granted by the disabled provider");
        assertTrue(failed[0], "the caller must be told immediately that nothing was shown");

        boolean[] closed = {false};
        provider.showInterstitial(() -> closed[0] = true);
        assertTrue(closed[0]);
        assertEquals(2, provider.showAttempts());
        provider.showPrivacyOptions();
        provider.dispose();
    }
}
