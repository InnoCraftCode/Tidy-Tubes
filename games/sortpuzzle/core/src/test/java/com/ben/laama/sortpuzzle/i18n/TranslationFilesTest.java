package com.ben.laama.sortpuzzle.i18n;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every English key used in the code ({@code T.r/f/raw/rawf/k/has("...")}) has a translation in
 * every language file, with the same {@code {n}} placeholders. Reads the sources and the
 * shipped {@code android/src/main/assets/i18n/*.json} (read-only); writes the key list to
 * {@code build/i18n/keys.txt} for translators.
 */
class TranslationFilesTest {

    private static final Path SOURCES = Paths.get("src/main/java");
    private static final Path FILES = Paths.get("../android/src/main/assets/i18n");
    private static final Pattern CALL = Pattern.compile(
        "T\\.(?:r|f|raw|rawf|k|has)\\(\\s*(\"(?:[^\"\\\\]|\\\\.)*\"(?:\\s*\\+\\s*\"(?:[^\"\\\\]|\\\\.)*\")*)");
    private static final Pattern LITERAL = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\d+\\}");

    static Set<String> keys() throws IOException {
        Set<String> keys = new TreeSet<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                Matcher call = CALL.matcher(source);
                while (call.find()) {
                    StringBuilder key = new StringBuilder();
                    Matcher literal = LITERAL.matcher(call.group(1));
                    while (literal.find()) key.append(unescape(literal.group(1)));
                    if (key.length() > 0) keys.add(key.toString());
                }
            }
        }
        keys.add(Names.ORDER);
        return keys;
    }

    private static String unescape(String text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\\' || i + 1 >= text.length()) {
                out.append(c);
                continue;
            }
            char e = text.charAt(++i);
            switch (e) {
                case 'n': out.append('\n'); break;
                case 't': out.append('\t'); break;
                case 'u':
                    out.append((char) Integer.parseInt(text.substring(i + 1, i + 5), 16));
                    i += 4;
                    break;
                default: out.append(e);
            }
        }
        return out.toString();
    }

    private static Set<String> placeholders(String text) {
        Set<String> found = new TreeSet<>();
        Matcher m = PLACEHOLDER.matcher(text);
        while (m.find()) found.add(m.group());
        return found;
    }

    @Test
    @DisplayName("every key used in the code is translated in every language, placeholders intact")
    void everyKeyIsTranslatedEverywhere() throws IOException {
        Set<String> keys = keys();
        Path out = Paths.get("build/i18n");
        Files.createDirectories(out);
        Files.write(out.resolve("keys.txt"), String.join("\n", keys).getBytes(StandardCharsets.UTF_8));

        List<String> problems = new ArrayList<>();
        for (Language language : Language.values()) {
            if (language == Language.EN) continue;
            Path file = FILES.resolve(language.code + ".json");
            if (!Files.exists(file)) {
                problems.add(language.code + ": file missing");
                continue;
            }
            Map<String, String> table = new LinkedHashMap<>();
            JsonValue root = new JsonReader().parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
            for (JsonValue e = root.child; e != null; e = e.next) table.put(e.name, e.asString());
            List<String> missing = new ArrayList<>();
            for (String key : keys) {
                String value = table.get(key);
                if (value == null || value.isEmpty()) {
                    missing.add(key);
                } else if (!placeholders(key).equals(placeholders(value))) {
                    problems.add(language.code + ": placeholders differ for \"" + key + "\" -> \"" + value + "\"");
                }
            }
            if (!missing.isEmpty()) {
                Files.write(out.resolve("missing-" + language.code + ".txt"),
                    String.join("\n", missing).getBytes(StandardCharsets.UTF_8));
                problems.add(language.code + ": " + missing.size() + " missing, e.g. \"" + missing.get(0) + "\"");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }
}
