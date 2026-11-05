package com.ben.laama.sortpuzzle.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.badlogic.gdx.files.FileHandle;

import com.ben.laama.sortpuzzle.BuildConfig;
import com.ben.laama.sortpuzzle.services.Share;

/**
 * The Android share sheet. The PNG lives in the app's private files directory and is exposed
 * read-only, for the one receiving app the player picks, through the {@code FileProvider}
 * declared in the manifest ({@code res/xml/share_paths.xml} limits it to {@code files/share/}).
 */
public final class AndroidShare implements Share {

    private static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".share";

    private final Activity activity;

    public AndroidShare(Activity activity) {
        this.activity = activity;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void shareImage(FileHandle png, String text, String title) {
        activity.runOnUiThread(() -> {
            Uri uri = FileProvider.getUriForFile(activity, AUTHORITY, png.file());
            Intent send = new Intent(Intent.ACTION_SEND)
                .setType("image/png")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_TEXT, text)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(send, title));
        });
    }
}
