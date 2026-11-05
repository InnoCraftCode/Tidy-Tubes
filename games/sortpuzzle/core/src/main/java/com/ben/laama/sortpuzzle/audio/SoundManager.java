package com.ben.laama.sortpuzzle.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.save.SaveData;

import java.util.Locale;

/**
 * Thin audio layer: one-shot cues, a looping music bed, and optional voice lines.
 *
 * <p>Every file is optional. For each cue the manager looks for {@code assets/sfx/<cue>.ogg},
 * then {@code .wav}; anything missing simply stays silent. The shipped effects and music are
 * synthesised by {@code tools/audio/SfxSynth.java} at the workspace root. Voice lines
 * ({@code voice_nice}, {@code voice_great}, ...) are not shipped: drop recordings in and they
 * play, no code change needed. Two independent flags live in {@link SaveData}: {@code effectsOff}
 * silences the effects, {@code musicOff} the music bed; the home screen's quick button flips
 * both at once.</p>
 */
public final class SoundManager implements Disposable {

    /** Every sound event the game can raise. */
    public enum Cue {
        PICK(0.45f),
        DROP(0.4f),
        INVALID(0.4f),
        WIN(0.55f),
        BUTTON(0.22f),
        COIN(0.35f),
        TUBE(0.45f),
        HINT(0.4f),
        START(0.25f),
        VOICE_NICE(1f),
        VOICE_GREAT(1f),
        VOICE_AWESOME(1f),
        VOICE_PERFECT(1f),
        VOICE_LEVEL_COMPLETE(1f);

        final float volume;

        Cue(float volume) {
            this.volume = volume;
        }
    }

    private static final String SFX_DIR = "sfx/";
    private static final String[] EXTENSIONS = {".ogg", ".wav"};
    private static final float MUSIC_VOLUME = 0.32f;
    /** Master gain for one-shot effects; the files themselves already peak at -8 dBFS. */
    private static final float EFFECTS_GAIN = 0.7f;

    private final SaveData save;
    private final ObjectMap<Cue, Sound> sounds = new ObjectMap<>();
    private Music music;
    private int loadedMusicTrack = -1;

    public SoundManager(SaveData save) {
        this.save = save;
        for (Cue cue : Cue.values()) {
            FileHandle handle = find(cue.name().toLowerCase(Locale.ROOT));
            if (handle != null) {
                try {
                    sounds.put(cue, Gdx.audio.newSound(handle));
                } catch (RuntimeException unavailable) {
                    Gdx.app.log("SoundManager", "Skipping " + handle.path() + ": " + unavailable.getMessage());
                }
            }
        }
    }

    private static FileHandle find(String name) {
        for (String extension : EXTENSIONS) {
            FileHandle handle = Gdx.files.internal(SFX_DIR + name + extension);
            if (handle.exists()) {
                return handle;
            }
        }
        return null;
    }

    public void play(Cue cue) {
        play(cue, 1f);
    }

    /**
     * @param pitch 0.5-2.0; used to make repeated cues musical (each ball landing a little
     *              higher than the last) instead of identical
     */
    public void play(Cue cue, float pitch) {
        if (save.effectsOff) return;
        Sound sound = sounds.get(cue);
        if (sound != null) {
            sound.play(cue.volume * EFFECTS_GAIN, MathUtils.clamp(pitch, 0.5f, 2f), 0f);
        }
    }

    /** @return whether a recording exists for this cue (used to pick between voice lines) */
    public boolean has(Cue cue) {
        return sounds.containsKey(cue);
    }

    /** Starts the selected looping music track if music is enabled. */
    public void startMusic() {
        if (save.musicTrack < 0) return;
        loadMusicTrack(save.musicTrack);
        if (music != null && isMusicOn() && !music.isPlaying()) {
            music.play();
        }
    }

    /** Plays a preview without changing the saved choice. */
    public void previewMusic(int track) {
        validateMusicTrack(track);
        loadMusicTrack(track);
        if (music != null && isMusicOn() && !music.isPlaying()) {
            music.play();
        }
    }

    /** Saves and starts the chosen track. */
    public void selectMusicTrack(int track) {
        validateMusicTrack(track);
        save.setMusicTrack(track);
        startMusic();
    }

    private void loadMusicTrack(int track) {
        if (music != null && loadedMusicTrack == track) return;
        if (music != null) {
            music.stop();
            music.dispose();
            music = null;
        }
        String basename = String.format(Locale.ROOT, "music_%02d", track + 1);
        FileHandle handle = find(basename);
        if (handle == null) {
            Gdx.app.error("SoundManager", "Selected music track is missing: " + basename);
            loadedMusicTrack = -1;
            return;
        }
        try {
            music = Gdx.audio.newMusic(handle);
            loadedMusicTrack = track;
            music.setLooping(true);
            music.setVolume(MUSIC_VOLUME);
        } catch (RuntimeException unavailable) {
            Gdx.app.error("SoundManager", "Could not load " + handle.path(), unavailable);
            loadedMusicTrack = -1;
        }
    }

    private static void validateMusicTrack(int track) {
        if (track < 0 || track >= GameConfig.RELAXING_MUSIC_TRACK_COUNT) {
            throw new IllegalArgumentException("unknown music track: " + track);
        }
    }

    /** @return whether everything is silent (effects and music both off) */
    public boolean isMuted() {
        return save.effectsOff && save.musicOff;
    }

    public boolean areEffectsOn() {
        return !save.effectsOff;
    }

    public boolean isMusicOn() {
        return !save.musicOff;
    }

    /** All audio on or off at once (the home screen's quick button). */
    public void setSoundEnabled(boolean enabled) {
        save.effectsOff = !enabled;
        save.setMusicOff(!enabled); // flushes both flags
        syncMusic();
    }

    /** Sound effects on or off, independently of the music. */
    public void setEffectsEnabled(boolean enabled) {
        save.setEffectsOff(!enabled);
    }

    /** Music bed on or off, independently of the effects. */
    public void setMusicEnabled(boolean enabled) {
        save.setMusicOff(!enabled);
        syncMusic();
    }

    /** Flips the mute flag, persists it, and stops or restarts the music to match. */
    public void toggleMute() {
        setSoundEnabled(isMuted());
    }

    private void syncMusic() {
        if (music == null) {
            if (isMusicOn() && save.musicTrack >= 0) {
                startMusic();
            }
            return;
        }
        if (isMusicOn()) {
            if (!music.isPlaying()) music.play();
        } else {
            music.pause();
        }
    }

    @Override
    public void dispose() {
        for (Sound sound : sounds.values()) {
            sound.dispose();
        }
        sounds.clear();
        if (music != null) {
            music.dispose();
            music = null;
        }
        loadedMusicTrack = -1;
    }
}
