package com.ben.laama.sortpuzzle.android;

import android.app.Activity;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.google.android.gms.games.AuthenticationResult;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.gms.tasks.Task;

import com.ben.laama.sortpuzzle.services.GameServices;

/**
 * Google Play Games Services v2: silent sign-in at start, a manual sign-in for the button, and
 * the leaderboards client. Everything talks to the SDK on the UI thread and reports back on the
 * render thread. Leaderboard IDs come from {@code playgames.properties} via BuildConfig.
 *
 * <p>API per https://developer.android.com/games/pgs/android/android-signin and
 * .../leaderboards (verified 2026-11-02): {@code PlayGamesSdk.initialize}, {@code
 * GamesSignInClient.isAuthenticated()/signIn()} returning {@link AuthenticationResult},
 * {@code LeaderboardsClient.submitScore/getLeaderboardIntent/getAllLeaderboardsIntent}, and the
 * leaderboard UI opened with {@code startActivityForResult} so the API can identify the caller.</p>
 */
public final class AndroidGameServices implements GameServices {

    private static final String TAG = "PlayGames";
    private static final int RC_LEADERBOARD_UI = 9004;

    /** Leaderboard IDs from Play Console (empty strings disable that board). */
    public static final class Config {
        final String highestLevelId;
        final String totalStarsId;
        final String dailyMovesId;

        public Config(String highestLevelId, String totalStarsId, String dailyMovesId) {
            this.highestLevelId = highestLevelId == null ? "" : highestLevelId.trim();
            this.totalStarsId = totalStarsId == null ? "" : totalStarsId.trim();
            this.dailyMovesId = dailyMovesId == null ? "" : dailyMovesId.trim();
        }
    }

    private final Activity activity;
    private final Config config;
    /** Written on the UI thread, read from the render thread. */
    private volatile boolean signedIn;
    private volatile String playerName = "";

    /** Call {@link #initializeSdk(Activity)} first, on the UI thread. */
    public AndroidGameServices(Activity activity, Config config) {
        this.activity = activity;
        this.config = config;
    }

    /** The SDK wants initialising once, before any client is created (Activity/Application onCreate). */
    public static void initializeSdk(Activity activity) {
        PlayGamesSdk.initialize(activity);
    }

    // ------------------------------------------------------------------ sign-in
    @Override
    public void initialize(SignInListener onSilentSignIn) {
        activity.runOnUiThread(() -> PlayGames.getGamesSignInClient(activity).isAuthenticated()
            .addOnCompleteListener(task -> onAuthentication(task, onSilentSignIn)));
    }

    @Override
    public void signIn(SignInListener listener) {
        activity.runOnUiThread(() -> PlayGames.getGamesSignInClient(activity).signIn()
            .addOnCompleteListener(task -> onAuthentication(task, listener)));
    }

    private void onAuthentication(Task<AuthenticationResult> task, SignInListener listener) {
        boolean ok = task.isSuccessful() && task.getResult() != null
            && task.getResult().isAuthenticated();
        signedIn = ok;
        if (ok) {
            PlayGames.getPlayersClient(activity).getCurrentPlayer()
                .addOnSuccessListener(player -> playerName = player.getDisplayName())
                .addOnFailureListener(e -> Log.w(TAG, "Player profile unavailable: " + e.getMessage()));
        } else {
            playerName = "";
            if (task.getException() != null) {
                Log.i(TAG, "Not signed in: " + task.getException().getMessage());
            }
        }
        if (listener != null) {
            Gdx.app.postRunnable(() -> listener.onResult(ok));
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isSignedIn() {
        return signedIn;
    }

    @Override
    public String playerName() {
        return playerName == null ? "" : playerName;
    }

    // ------------------------------------------------------------- leaderboards
    @Override
    public void submitScore(Leaderboard board, long score) {
        String id = idFor(board);
        if (!signedIn || id.isEmpty()) {
            return;
        }
        // submitScoreImmediate: the plain submitScore is fire-and-forget and swallows errors,
        // which makes a misconfigured board (wrong id, non-tester account) impossible to see.
        activity.runOnUiThread(() -> PlayGames.getLeaderboardsClient(activity)
            .submitScoreImmediate(id, score)
            .addOnSuccessListener(result -> Log.i(TAG, "score " + score + " -> " + board))
            .addOnFailureListener(e -> Log.w(TAG, "score " + score + " -> " + board
                + " refused: " + e.getMessage())));
    }

    @Override
    public void showLeaderboard(Leaderboard board) {
        String id = idFor(board);
        if (!signedIn || id.isEmpty()) {
            return;
        }
        activity.runOnUiThread(() -> PlayGames.getLeaderboardsClient(activity)
            .getLeaderboardIntent(id)
            .addOnSuccessListener(intent -> activity.startActivityForResult(intent, RC_LEADERBOARD_UI))
            .addOnFailureListener(e -> Log.w(TAG, "Leaderboard UI: " + e.getMessage())));
    }

    @Override
    public void showLeaderboards() {
        if (!signedIn) {
            return;
        }
        activity.runOnUiThread(() -> PlayGames.getLeaderboardsClient(activity)
            .getAllLeaderboardsIntent()
            .addOnSuccessListener(intent -> activity.startActivityForResult(intent, RC_LEADERBOARD_UI))
            .addOnFailureListener(e -> Log.w(TAG, "Leaderboards UI: " + e.getMessage())));
    }

    private String idFor(Leaderboard board) {
        switch (board) {
            case HIGHEST_LEVEL:
                return config.highestLevelId;
            case TOTAL_STARS:
                return config.totalStarsId;
            case DAILY_FEWEST_MOVES:
                return config.dailyMovesId;
            default:
                return "";
        }
    }
}
