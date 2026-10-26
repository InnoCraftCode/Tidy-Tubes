package com.ben.laama.sortpuzzle.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

import com.ben.laama.sortpuzzle.save.SaveData;

import java.util.Locale;

/**
 * Thin audio layer: one-shot cues, a looping music bed, and optional voice lines.
 *
 * <p>Every file is optional. For each cue the manager looks for {@code assets/sfx/<cue>.ogg},
 * then {@code .wav}; anything missing simply stays silent. The shipped effects and music are
 * synthesised by {@code tools/audio/SfxSynth.java} at the workspace root. Voice lines
 * ({@code voice_nice}, {@code voice_great}, ...) are not shipped: drop recordings in and they
 * play, no code change needed. Two flags live in {@link SaveData}: {@code muted} silences
 * everything, {@code musicOff} only the music bed.</p>
 */
public final class SoundManager implements Disposable {

    /** Every sound event the game can raise. */
    public enum Cue {
        PICK(0.55f),
        DROP(0.6f),
        INVALID(0.5f),
        WIN(0.9f),
        BUTTON(0.4f),
        COIN(0.6f),
        TUBE(0.75f),
        HINT(0.5f),
        START(0.35f),
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

    private final SaveData save;
    private final ObjectMap<Cue, Sound> sounds = new ObjectMap<>();
    private Music music;

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
        if (save.muted) return;
        Sound sound = sounds.get(cue);
        if (sound != null) {
            sound.play(cue.volume, MathUtils.clamp(pitch, 0.5f, 2f), 0f);
        }
    }

    /** @return whether a recording exists for this cue (used to pick between voice lines) */
    public boolean has(Cue cue) {
        return sounds.containsKey(cue);
    }

    /** Starts the looping music bed if a {@code music} file exists and music is on. */
    public void startMusic() {
        if (music == null) {
            FileHandle handle = find("music");
            if (handle == null) {
                return;
            }
            try {
                music = Gdx.audio.newMusic(handle);
            } catch (RuntimeException unavailable) {
                Gdx.app.log("SoundManager", "No music: " + unavailable.getMessage());
                return;
            }
            music.setLooping(true);
            music.setVolume(MUSIC_VOLUME);
        }
        if (isMusicOn() && !music.isPlaying()) {
            music.play();
        }
    }

    public boolean isMuted() {
        return save.muted;
    }

    public boolean isMusicOn() {
        return !save.muted && !save.musicOff;
    }

    /** All audio on or off; persists and starts or pauses the music to match. */
    public void setSoundEnabled(boolean enabled) {
        save.setMuted(!enabled);
        syncMusic();
    }

    /** Music bed on or off, independently of the effects. */
    public void setMusicEnabled(boolean enabled) {
        save.setMusicOff(!enabled);
        syncMusic();
    }

    /** Flips the mute flag, persists it, and stops or restarts the music to match. */
    public void toggleMute() {
        setSoundEnabled(save.muted);
    }

    private void syncMusic() {
        if (music == null) {
            if (isMusicOn()) {
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
    }
}
