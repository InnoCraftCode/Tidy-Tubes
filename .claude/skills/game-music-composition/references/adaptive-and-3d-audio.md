# Adaptive music, ducking, crossfades and pseudo-3D audio in libGDX

Everything here is pure `com.badlogic.gdx.audio` plus a small plain-Java mixer class with unit
tests. No native plugins, no FMOD/Wwise: they do not fit a 5 MB casual game and their licences
need a legal pass.

## 1. Layered (vertical) music

Generate stems with `MusicSynth.java --stems`. Each stem is a `Music` instance started at the
same moment, all looping, each with its own volume. Intensity is a float 0-1 mapped to layer
volumes; the mapping lives in logic and is tested, the screen only pushes `intensity`.

```java
/** Pure logic, core/.../audio/LayerMix.java - unit test it. */
public final class LayerMix {
    /** Volume of each stem (pad, bass, melody, drums) for an intensity in [0, 1]. */
    public static float[] volumes(float intensity) {
        float i = MathUtils.clamp(intensity, 0f, 1f);
        return new float[] {
            1f,                                   // pad: always
            MathUtils.clamp(i * 2f, 0f, 1f),      // bass fades in over the first half
            MathUtils.clamp((i - 0.3f) * 2f, 0f, 1f),
            MathUtils.clamp((i - 0.6f) * 2.5f, 0f, 1f) // drums only near the top
        };
    }
}
```

Driver (in SoundManager, not in screens):

```java
private final Music[] stems = new Music[4];
private final float[] target = new float[4], current = new float[4];

public void setIntensity(float v) { System.arraycopy(LayerMix.volumes(v), 0, target, 0, 4); }

/** Call from render(delta): slews volumes so a jump in intensity never clicks. */
public void update(float delta) {
    float step = delta / 0.6f;                       // ~600 ms to reach a new target
    for (int i = 0; i < stems.length; i++) {
        if (stems[i] == null) continue;
        current[i] = MathUtils.lerp(current[i], target[i], Math.min(1f, step));
        stems[i].setVolume(current[i] * MUSIC_VOLUME);
    }
}
```

Stems drift apart on Android after minutes because each `Music` has its own decoder clock.
Mitigation: restart all stems together on every loop of the pad via
`stems[0].setOnCompletionListener(...)` only if `setLooping(false)` is used, or simply restart
them together at natural breaks (level start). Three or four stems is the practical maximum:
MediaPlayer-backed `Music` instances are heavy, and more than four is where OEM devices start
failing to create new players.

## 2. Horizontal transitions (menu -> play, world A -> world B)

Crossfade with two `Music` instances, then dispose the old one. 400-800 ms is enough; longer
fades make the player wait for the new mood.

```java
public void crossfadeTo(String basename, float seconds) {
    Music next = load(basename);                   // null if the file is missing -> just stop old
    if (next == null) { stopMusic(); return; }
    next.setLooping(true); next.setVolume(0f); next.play();
    fadeOut = music; fadeIn = next; fadeLeft = fadeTotal = seconds;
    music = next;
}
// in update(delta):
if (fadeIn != null) {
    fadeLeft = Math.max(0f, fadeLeft - delta);
    float k = 1f - fadeLeft / fadeTotal;
    fadeIn.setVolume(k * MUSIC_VOLUME);
    if (fadeOut != null) fadeOut.setVolume((1f - k) * MUSIC_VOLUME);
    if (fadeLeft == 0f) { if (fadeOut != null) { fadeOut.stop(); fadeOut.dispose(); } fadeOut = null; fadeIn = null; }
}
```

Musical alternative when stems share tempo: wait for the next bar boundary before switching.
Bar length in seconds is `240f / bpm`; keep a `playedSeconds` counter from `Music.getPosition()`.

## 3. Ducking (music dips under an important cue)

Rewarded-ad start, level win, voice line: drop the bed to 40 % over 100 ms, hold, return over
600 ms. Implement as a second multiplier on the music volume so it composes with fades and the
settings volume:

```java
private float duck = 1f, duckTarget = 1f;
public void duck(boolean on) { duckTarget = on ? 0.4f : 1f; }
// update(delta): duck = MathUtils.lerp(duck, duckTarget, Math.min(1f, delta / (duckTarget < duck ? 0.1f : 0.6f)));
// final volume = base * fadeK * duck * MUSIC_VOLUME
```

Ads: `AdProvider` callbacks already signal show/dismiss; pause the bed entirely while an ad is
on screen (AdMob plays its own audio; two beds is a policy and UX fault) and resume on dismiss.

## 4. Pseudo-3D positional audio

libGDX exposes stereo pan and volume only (`Sound.play(volume, pitch, pan)`,
`Music.setPan(pan, volume)`); there is no HRTF or distance model on Android. Compute both from
the listener (camera) and the emitter in plain Java and test it:

```java
/** core/.../audio/Spatial.java - pure maths, unit-tested. */
public final class Spatial {
    public static final float REF = 2f, MAX = 30f;   // world units: full volume within REF, silent past MAX

    public static float volume(float distance) {
        if (distance <= REF) return 1f;
        if (distance >= MAX) return 0f;
        float t = (distance - REF) / (MAX - REF);
        return (1f - t) * (1f - t);                // inverse-square-ish, cheap
    }

    /** -1 left .. +1 right from the listener's right vector and the emitter offset. */
    public static float pan(Vector3 listenerRight, Vector3 offset) {
        float len = offset.len();
        if (len < 1e-4f) return 0f;
        return MathUtils.clamp(listenerRight.dot(offset) / len, -0.8f, 0.8f); // never hard-panned: phone speakers are mono
    }
}
```

For 2D games the same functions apply with `distance = |emitterX - cameraX| / viewportWidth`
scaled and `pan = (emitterX - cameraX) / (viewportWidth / 2)`.

Practical limits:

- Fire-and-forget sounds get their pan at `play()`; moving emitters (an engine, a river) need a
  looping `Sound` id updated with `setPan(id, pan, volume)` every ~100 ms, not every frame.
- Many Android decoders ignore `setPan` on mono sources at runtime; keep looping emitters as
  stereo files if panning must change live, or accept volume-only attenuation.
- A single listener. Split-screen is not a mobile case.
- Doppler, occlusion and reverb zones: not worth it below "3D shooter" scope; a 7 kHz low-pass
  version of the sound played when occluded is the only cheap trick that reads clearly.

## 5. Verification matrix

| Scenario | Expected |
|---|---|
| Intensity 0 -> 1 in one frame | volumes slew over ~600 ms, no click |
| Switch screens during a crossfade | old fade completes or is cut, never two beds left playing; count `Music` instances in a debug overlay |
| Rewarded ad shown | bed paused, resumed only if it was playing and `musicOff` is still false |
| App to background mid-crossfade | both instances paused; resume restores the slewed volumes |
| Emitter crosses the listener | pan passes through 0, no jump between -0.8 and +0.8 |
| 10 minutes of play with stems | stems still aligned (listen for flam); if not, re-sync at level start |
