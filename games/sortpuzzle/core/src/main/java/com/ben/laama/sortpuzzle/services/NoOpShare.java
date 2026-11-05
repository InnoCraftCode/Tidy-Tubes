package com.ben.laama.sortpuzzle.services;

import com.badlogic.gdx.files.FileHandle;

/** Desktop and tests: no share sheet, the button explains itself with a toast. */
public final class NoOpShare implements Share {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void shareImage(FileHandle png, String text, String title) {
    }
}
