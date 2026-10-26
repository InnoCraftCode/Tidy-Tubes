package com.example.gametemplate.ads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RewardGate")
class RewardGateTest {

    private static final class Recorder implements AdProvider.RewardListener {
        int rewarded;
        int failed;

        @Override
        public void onRewarded() {
            rewarded++;
        }

        @Override
        public void onFailed() {
            failed++;
        }
    }

    @Test
    @DisplayName("earned then dismissed grants exactly one reward")
    void earnedThenDismissed() {
        Recorder r = new Recorder();
        RewardGate gate = new RewardGate(r);
        gate.markEarned();
        gate.onDismissed();
        gate.onDismissed();
        gate.onFailed();
        assertEquals(1, r.rewarded);
        assertEquals(0, r.failed);
        assertTrue(gate.isSettled());
    }

    @Test
    @DisplayName("dismissed early (never earned) fails once and never rewards")
    void dismissedEarly() {
        Recorder r = new Recorder();
        RewardGate gate = new RewardGate(r);
        gate.onDismissed();
        gate.markEarned(); // a late earn after settlement is ignored
        gate.onDismissed();
        assertEquals(0, r.rewarded);
        assertEquals(1, r.failed);
    }

    @Test
    @DisplayName("a failure with no earn reports failure exactly once")
    void failure() {
        Recorder r = new Recorder();
        RewardGate gate = new RewardGate(r);
        gate.onFailed();
        gate.onFailed();
        assertEquals(0, r.rewarded);
        assertEquals(1, r.failed);
    }
}
