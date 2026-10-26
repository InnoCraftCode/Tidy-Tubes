package com.example.gametemplate.logic;

/** A circle the player has to tap before it expires. Plain Java, no libGDX. */
public final class Target {

    public final float x;
    public final float y;
    public final float radius;
    private final float lifetime;
    private float age;

    public Target(float x, float y, float radius, float lifetime) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.lifetime = lifetime;
    }

    void age(float delta) {
        age += delta;
    }

    public boolean expired() {
        return age >= lifetime;
    }

    /** 1 when fresh, 0 when about to expire. Drives the shrink animation. */
    public float remaining() {
        return Math.max(0f, 1f - age / lifetime);
    }

    public boolean contains(float px, float py) {
        float dx = px - x;
        float dy = py - y;
        return dx * dx + dy * dy <= radius * radius;
    }
}
