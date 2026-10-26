package com.example.gametemplate.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import com.example.gametemplate.GameConfig;
import com.example.gametemplate.TemplateGame;

/** Desktop launcher for fast iteration. Opens at half the virtual resolution; resizable. */
public final class Lwjgl3Launcher {

    private Lwjgl3Launcher() {
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Game Template");
        config.setWindowedMode((int) (GameConfig.WORLD_WIDTH / 2), (int) (GameConfig.WORLD_HEIGHT / 2));
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new TemplateGame(), config);
    }
}
