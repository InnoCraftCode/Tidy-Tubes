package com.ben.laama.sortpuzzle.ads;

/**
 * Whether this release of the game carries ads at all.
 *
 * <p>{@code true}: the Android launcher wires AdMob (GMA Next-Gen SDK + UMP consent) through
 * {@code AdMobAdProvider}, rewarded videos pay for hint unlocks and extra moves, and paced
 * interstitials run at level breaks. Per-build availability is still decided by Gradle:
 * debug builds use Google's demo unit IDs, release builds read production IDs from the
 * git-ignored {@code admob.properties} and ship with ads <em>off</em> when that file is absent,
 * and {@code -Pads.enabled=false} turns ads off in any build. A build without ads gets
 * {@link DisabledAdProvider}; the screens then offer coins instead of videos.</p>
 *
 * <p>{@code false}: no provider is ever created on any platform ({@link AdProviders}). The SDK
 * would still be on the Android classpath; remove the dependency as well for a truly ad-free
 * build. See {@code docs/admob-integration-current-release.md}.</p>
 */
public final class AdsConfig {

    private AdsConfig() {
    }

    /** The current release ships with AdMob. */
    public static final boolean ADS_ENABLED = true;
}
