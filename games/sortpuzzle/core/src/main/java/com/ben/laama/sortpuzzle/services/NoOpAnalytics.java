package com.ben.laama.sortpuzzle.services;

/** Drops every event. Desktop, tests, and Android builds without a Firebase configuration. */
public final class NoOpAnalytics implements Analytics {

    @Override
    public void event(String name, Object... keyValues) {
    }
}
