package com.ben.laama.sortpuzzle.services;

import com.badlogic.gdx.files.FileHandle;

/**
 * Hands an image and a line of text to the platform's share sheet. The only way a solo
 * puzzle spreads is a player showing a result to a friend, so this is wired to every win.
 * Inert off Android ({@link NoOpShare}).
 */
public interface Share {

    /** @return whether a share sheet exists on this platform */
    boolean isAvailable();

    /**
     * Opens the share sheet with the image attached and the text pre-filled.
     *
     * @param png   a PNG inside {@code Gdx.files.local("share/")}, the only folder exposed
     * @param text  the message, store link included
     * @param title the chooser's title
     */
    void shareImage(FileHandle png, String text, String title);
}
