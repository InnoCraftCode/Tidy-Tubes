package com.ben.laama.sortpuzzle.services;

/** Desktop, tests and Android builds without a Play Games configuration: nothing is available. */
public final class NoOpGameServices implements GameServices {

    @Override
    public void initialize(SignInListener onSilentSignIn) {
        if (onSilentSignIn != null) {
            onSilentSignIn.onResult(false);
        }
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public boolean isSignedIn() {
        return false;
    }

    @Override
    public String playerName() {
        return "";
    }

    @Override
    public void signIn(SignInListener listener) {
        if (listener != null) {
            listener.onResult(false);
        }
    }

    @Override
    public void submitScore(Leaderboard board, long score) {
    }

    @Override
    public void showLeaderboard(Leaderboard board) {
    }

    @Override
    public void showLeaderboards() {
    }
}
