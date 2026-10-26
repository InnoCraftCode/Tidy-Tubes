package com.example.gametemplate.ads;

/**
 * Turns the ad SDK's loose event stream into exactly one {@link AdProvider.RewardListener} call.
 *
 * <p>SDKs report "reward earned" and "ad dismissed" as separate events, in either order, possibly
 * on different threads, and failure paths can fire after a success. This gate records the earn
 * signal and settles once — on dismiss or failure — so a reward can never be granted twice or
 * granted for an ad that was closed early. Plain Java so it is unit-tested.</p>
 */
public final class RewardGate {

    private final AdProvider.RewardListener listener;
    private boolean earned;
    private boolean settled;

    public RewardGate(AdProvider.RewardListener listener) {
        this.listener = listener;
    }

    /** The network says the user watched long enough. Does not settle on its own. */
    public synchronized void markEarned() {
        if (!settled) {
            earned = true;
        }
    }

    /** The ad was dismissed: reward if earned, otherwise fail. Later calls are ignored. */
    public void onDismissed() {
        settle();
    }

    /** The ad failed to load or show. Grants a reward only if one was already earned. */
    public void onFailed() {
        settle();
    }

    private void settle() {
        boolean reward;
        synchronized (this) {
            if (settled) {
                return;
            }
            settled = true;
            reward = earned;
        }
        if (reward) {
            listener.onRewarded();
        } else {
            listener.onFailed();
        }
    }

    public synchronized boolean isSettled() {
        return settled;
    }
}
