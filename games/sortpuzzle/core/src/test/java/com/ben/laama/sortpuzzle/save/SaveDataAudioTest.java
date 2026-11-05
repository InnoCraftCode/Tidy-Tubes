package com.ben.laama.sortpuzzle.save;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SaveData audio switches")
class SaveDataAudioTest {

    @Test
    @DisplayName("a fresh profile has effects and music on")
    void defaultsOn() {
        SaveData save = new SaveData();
        assertFalse(save.effectsOff);
        assertFalse(save.musicOff);
    }

    @Test
    @DisplayName("an old save with everything muted stays silent after the update")
    void legacyMuteBecomesBothOff() {
        SaveData save = new SaveData();
        save.muted = true;
        assertTrue(save.migrateAudioFlags());
        assertTrue(save.effectsOff);
        assertTrue(save.musicOff);
        assertFalse(save.muted);
        assertFalse(save.migrateAudioFlags(), "migration runs once");
    }

    @Test
    @DisplayName("an old save with only the music off keeps the effects on")
    void legacyMusicOnlyKept() {
        SaveData save = new SaveData();
        save.musicOff = true;
        assertFalse(save.migrateAudioFlags());
        assertFalse(save.effectsOff);
        assertTrue(save.musicOff);
    }

    @Test
    @DisplayName("effects and music switch independently")
    void independent() {
        SaveData save = new SaveData();
        save.setEffectsOff(true);
        assertTrue(save.effectsOff);
        assertFalse(save.musicOff);
        save.setEffectsOff(false);
        save.setMusicOff(true);
        assertFalse(save.effectsOff);
        assertTrue(save.musicOff);
    }
}
