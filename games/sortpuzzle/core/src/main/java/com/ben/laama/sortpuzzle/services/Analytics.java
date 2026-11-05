package com.ben.laama.sortpuzzle.services;

/**
 * Product analytics sink (Firebase Analytics on Android, nothing elsewhere).
 *
 * <p>Event and parameter names come from {@link AnalyticsEvents} only, so they stay within the
 * provider's limits and never carry free text, identifiers or anything personal. Whether events
 * are actually collected is decided by the platform layer from the user's consent; the core
 * just reports what happened.</p>
 */
public interface Analytics {

    /**
     * @param name      an {@link AnalyticsEvents} event constant
     * @param keyValues alternating parameter name (an {@link AnalyticsEvents} param constant)
     *                  and value ({@code String}, {@code Integer}, {@code Long}, {@code Boolean}
     *                  or {@code Double})
     */
    void event(String name, Object... keyValues);
}
