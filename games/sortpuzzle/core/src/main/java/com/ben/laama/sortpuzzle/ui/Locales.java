package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import com.ben.laama.sortpuzzle.i18n.Language;
import com.ben.laama.sortpuzzle.i18n.T;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads {@code i18n/<code>.json} (a flat object: English text to translation) and installs it
 * in {@link T}. English has no file: its keys are its texts.
 */
public final class Locales {

    private Locales() {
    }

    public static void install(Language language) {
        Map<String, String> table = new HashMap<>();
        if (language != Language.EN) {
            FileHandle file = Gdx.files.internal("i18n/" + language.code + ".json");
            if (file.exists()) {
                try {
                    JsonValue root = new JsonReader().parse(file);
                    for (JsonValue entry = root.child; entry != null; entry = entry.next) {
                        if (entry.isString()) {
                            table.put(entry.name, entry.asString());
                        }
                    }
                } catch (RuntimeException broken) {
                    Gdx.app.error("Locales", "Unreadable " + file.path() + ": " + broken.getMessage());
                }
            } else {
                Gdx.app.error("Locales", "Missing " + file.path());
            }
        }
        T.install(language, table);
    }
}
