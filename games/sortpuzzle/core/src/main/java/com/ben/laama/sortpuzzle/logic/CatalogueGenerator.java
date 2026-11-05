package com.ben.laama.sortpuzzle.logic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Fills each cosmetic category up to {@link Cosmetic#PER_CATEGORY} items from families of
 * parameters: deterministic (no randomness, same catalogue on every device and every run),
 * pure Java, and checked as it goes against the same readability rules the catalogue test
 * enforces (ten distinguishable ball colours, balls visible on every backdrop, text readable
 * on every theme).
 *
 * <p><b>Ids are part of the save.</b> Generated ids are {@code <category>.gNNN} in generation
 * order: only ever append families or raise the count, never reorder or change a shipped
 * item's index, or players lose what they own.</p>
 *
 * <p>Unlocks, per category, in blocks of 20 generated items: 12 for coins (prices rising
 * through {@link #PRICE_TIERS}), 5 on the star road, 3 as world-chapter rewards - about
 * 60 / 25 / 15 %. Star thresholds and chapter numbers are dealt across the categories in
 * turn, so every milestone brings a different kind of reward.</p>
 */
final class CatalogueGenerator {

    static final int[] PRICE_TIERS = {250, 300, 400, 500, 600, 750, 900, 1200, 1500};
    /** Generated star milestones: 45, 65, 85, ... (never on a hand-made one, which end in 0). */
    static final int FIRST_STARS = 45;
    static final int STAR_STEP = 20;

    static final String TEXT = "F4F6FF";
    static final double MIN_TEXT_CONTRAST = 7.0;
    static final double MIN_BALL_DISTANCE = 50.0;
    static final double MIN_BALL_ON_BACKDROP = 60.0;

    private static final String[] HUE_NAMES = {
        "Ruby", "Amber", "Honey", "Lime", "Jade", "Mint",
        "Teal", "Azure", "Indigo", "Violet", "Orchid", "Rose"};

    private CatalogueGenerator() {
    }

    private enum Kind { COINS, STARS, CHAPTER }

    private static Kind kindOf(int index) {
        int slot = index % 20;
        return slot < 12 ? Kind.COINS : slot < 17 ? Kind.STARS : Kind.CHAPTER;
    }

    static List<Cosmetic> generate(List<Cosmetic> handmade, int perCategory) {
        Cosmetic.Category[] categories = Cosmetic.Category.values();
        int[] counts = new int[categories.length];
        for (int c = 0; c < categories.length; c++) {
            int existing = 0;
            for (Cosmetic item : handmade) {
                if (item.category() == categories[c]) existing++;
            }
            counts[c] = Math.max(0, perCategory - existing);
        }

        // Deal star thresholds and chapter numbers across categories, index by index.
        int[][] requirement = new int[categories.length][];
        int[] coinTotals = new int[categories.length];
        for (int c = 0; c < categories.length; c++) {
            requirement[c] = new int[counts[c]];
            for (int j = 0; j < counts[c]; j++) {
                if (kindOf(j) == Kind.COINS) coinTotals[c]++;
            }
        }
        int starRank = 0;
        int chapter = 1;
        int longest = 0;
        for (int count : counts) longest = Math.max(longest, count);
        for (int j = 0; j < longest; j++) {
            for (int c = 0; c < categories.length; c++) {
                if (j >= counts[c]) continue;
                Kind kind = kindOf(j);
                if (kind == Kind.STARS) {
                    requirement[c][j] = FIRST_STARS + STAR_STEP * starRank++;
                } else if (kind == Kind.CHAPTER) {
                    requirement[c][j] = chapter++;
                }
            }
        }

        List<String> themeTops = new ArrayList<>();
        List<String[]> palettes = new ArrayList<>();
        for (Cosmetic item : handmade) {
            if (item.category() == Cosmetic.Category.THEME) themeTops.add(item.hex(0));
            if (item.category() == Cosmetic.Category.BALL_PALETTE) palettes.add(item.hex());
        }

        List<Cosmetic> out = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (Cosmetic item : handmade) names.add(item.category() + item.displayName());

        int[] coinRank = new int[categories.length];
        for (int c = 0; c < categories.length; c++) {
            Cosmetic.Category category = categories[c];
            for (int j = 0; j < counts[c]; j++) {
                Kind kind = kindOf(j);
                int price = 0;
                Cosmetic.Unlock unlock = Cosmetic.Unlock.NONE;
                int need = 0;
                if (kind == Kind.COINS) {
                    int tier = Math.min(PRICE_TIERS.length - 1,
                        coinRank[c]++ * PRICE_TIERS.length / Math.max(1, coinTotals[c]));
                    price = PRICE_TIERS[tier];
                } else {
                    unlock = kind == Kind.STARS ? Cosmetic.Unlock.STARS : Cosmetic.Unlock.CHAPTER;
                    need = requirement[c][j];
                }
                String id = prefix(category) + String.format(Locale.ROOT, "g%03d", j + 1);
                switch (category) {
                    case BALL_PALETTE: {
                        String[] colours = paletteColours(j, themeTops);
                        palettes.add(colours);
                        out.add(Cosmetic.palette(id, unique(names, category, paletteName(j)),
                            price, unlock, need, paletteDescription(j), colours));
                        break;
                    }
                    case BALL_FINISH:
                        out.add(finish(id, j, price, unlock, need, names));
                        break;
                    case TUBE:
                        out.add(tube(id, j, price, unlock, need, names));
                        break;
                    default:
                        out.add(theme(id, j, price, unlock, need, names, palettes));
                        break;
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ palettes
    private static final float[] PALETTE_HUES = {0f, 28f, 52f, 120f, 172f, 205f, 262f, 315f};
    private static final String[] PALETTE_FAMILIES = {"Vivid", "Soft", "Jewel", "Pastel", "Neon", "Sunny"};
    private static final float[][] PALETTE_SV = {
        {0.85f, 1.00f}, {0.58f, 1.00f}, {0.80f, 0.86f}, {0.45f, 1.00f}, {1.00f, 1.00f}, {0.72f, 0.96f}};
    private static final String[] PALETTE_NOUNS = {
        "Pop", "Bloom", "Splash", "Burst", "Drops", "Mix", "Glow", "Beads", "Gems", "Sweets",
        "Fizz", "Petals", "Prism", "Confetti", "Marbles", "Lights"};

    private static String paletteName(int j) {
        return PALETTE_FAMILIES[j % PALETTE_FAMILIES.length] + " "
            + PALETTE_NOUNS[(j / PALETTE_FAMILIES.length) % PALETTE_NOUNS.length];
    }

    private static String paletteDescription(int j) {
        switch (j % PALETTE_FAMILIES.length) {
            case 0: return "Ten bold, saturated colours";
            case 1: return "Ten gentle, airy colours";
            case 2: return "Ten deep jewel tones";
            case 3: return "Ten pale pastel colours";
            case 4: return "Ten electric neon colours";
            default: return "Ten warm, sunlit colours";
        }
    }

    /** Ten colours: eight hues around the wheel, a light neutral and an earth tone. */
    static String[] paletteColours(int j, List<String> themeTops) {
        int family = j % PALETTE_FAMILIES.length;
        float shift = ((j * 11) % 21) - 10f;
        float saturation = PALETTE_SV[family][0];
        float value = PALETTE_SV[family][1];
        for (int attempt = 0; attempt < 12; attempt++) {
            String[] colours = new String[10];
            for (int i = 0; i < PALETTE_HUES.length; i++) {
                colours[i] = hsv(PALETTE_HUES[i] + shift, saturation, value);
            }
            colours[8] = hsv(210f + shift * 3f, 0.08f, 0.95f);
            colours[9] = hsv(26f + shift, 0.58f, 0.64f);
            if (paletteOk(colours, themeTops)) {
                return colours;
            }
            saturation = Math.min(1f, saturation + 0.07f);   // pull close hues apart
            value = Math.min(1f, value + 0.03f);
        }
        throw new IllegalStateException("palette family " + j + " cannot meet the contrast rules");
    }

    static boolean paletteOk(String[] colours, List<String> themeTops) {
        for (int a = 0; a < colours.length; a++) {
            for (int b = a + 1; b < colours.length; b++) {
                if (Contrast.distance(colours[a], colours[b]) < MIN_BALL_DISTANCE) return false;
            }
            for (String top : themeTops) {
                if (Contrast.distance(colours[a], top) < MIN_BALL_ON_BACKDROP) return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ finishes
    private static final String[] FINISH_BASES = {
        "finish.glossy", "finish.matte", "finish.glass", "finish.gem", "finish.jelly", "finish.chrome"};
    private static final String[] FINISH_BASE_NAMES = {"Glossy", "Matte", "Glass", "Gem", "Jelly", "Chrome"};
    private static final Cosmetic.Pattern[] PATTERNS = {
        Cosmetic.Pattern.BANDS, Cosmetic.Pattern.STRIPES, Cosmetic.Pattern.RING, Cosmetic.Pattern.DOTS,
        Cosmetic.Pattern.SWIRL, Cosmetic.Pattern.DUO, Cosmetic.Pattern.CHECKER, Cosmetic.Pattern.SPARKLE};
    private static final String[] PATTERN_PHRASES = {
        "soft bands", "diagonal stripes", "a billiard ring", "polka dots", "a swirl",
        "two tones", "a checker pattern", "sparkles"};
    private static final int[][] PATTERN_COUNTS ={{3, 5}, {4, 6}, {1, 2}, {3, 4}, {3, 5}, {1, 2}, {3, 4}, {10, 18}};

    private static Cosmetic finish(String id, int j, int price, Cosmetic.Unlock unlock, int need,
                                   Set<String> names) {
        int patternIndex = j % PATTERNS.length;
        int base = (j / PATTERNS.length) % FINISH_BASES.length;
        int variant = Math.min(1, j / (PATTERNS.length * FINISH_BASES.length));
        Cosmetic.Pattern pattern = PATTERNS[patternIndex];
        String name = FINISH_BASE_NAMES[base] + " " + pattern.title() + (variant == 1 ? " II" : "");
        String description = FINISH_BASE_NAMES[base] + " shine with " + PATTERN_PHRASES[patternIndex]
            + (variant == 1 ? ", bolder" : "");
        return Cosmetic.finish(id, unique(names, Cosmetic.Category.BALL_FINISH, name), price,
            unlock, need, description, FINISH_BASES[base], pattern,
            PATTERN_COUNTS[patternIndex][variant], 0.2f + 0.08f * variant);
    }

    // --------------------------------------------------------------------- tubes
    private static final String[] TUBE_FAMILIES = {"Tint", "Neon", "Smoke", "Frost", "Halo"};

    private static Cosmetic tube(String id, int j, int price, Cosmetic.Unlock unlock, int need,
                                 Set<String> names) {
        float hue = (j * 137.508f + 12f) % 360f;
        int family = j % TUBE_FAMILIES.length;
        String body;
        String rim;
        float thickness;
        String description;
        switch (family) {
            case 0:
                body = hsv(hue, 0.5f, 1f) + "20"; rim = hsv(hue, 0.6f, 1f) + "E6"; thickness = 3f;
                description = "Tinted glass, matching rim";
                break;
            case 1:
                body = "FFFFFF12"; rim = hsv(hue, 0.85f, 1f) + "F2"; thickness = 4.2f;
                description = "Clear glass, glowing rim";
                break;
            case 2:
                body = "00000040"; rim = hsv(hue, 0.35f, 1f) + "D0"; thickness = 3f;
                description = "Smoked glass, coloured edge";
                break;
            case 3:
                body = hsv(hue, 0.2f, 1f) + "30"; rim = hsv(hue, 0.25f, 1f) + "70"; thickness = 2.4f;
                description = "Frosted glass, soft edge";
                break;
            default:
                body = hsv(hue, 0.6f, 1f) + "16"; rim = hsv(hue, 0.5f, 1f) + "F0"; thickness = 5f;
                description = "Bold halo rim";
                break;
        }
        String name = hueName(hue) + " " + TUBE_FAMILIES[family];
        return Cosmetic.tube(id, unique(names, Cosmetic.Category.TUBE, name), price, unlock, need,
            description, thickness, body, rim);
    }

    // -------------------------------------------------------------------- themes
    private static final String[] THEME_FAMILIES = {"Dusk", "Deep", "Mist", "Ink"};
    private static final float[][] THEME_SV = {
        {0.55f, 0.32f, 0.10f}, {0.75f, 0.26f, 0.07f}, {0.32f, 0.30f, 0.10f}, {0.45f, 0.18f, 0.05f}};
    private static final String[] PLACES = {
        "Harbor", "Canyon", "Lagoon", "Tundra", "Meadow", "Glacier", "Orchard", "Dune",
        "Reef", "Grove", "Summit", "Valley", "Delta", "Fjord", "Prairie", "Atoll",
        "Bayou", "Mesa", "Steppe", "Marsh", "Cove", "Ridge", "Isle", "Haven"};

    private static Cosmetic theme(String id, int j, int price, Cosmetic.Unlock unlock, int need,
                                  Set<String> names, List<String[]> palettes) {
        float hue = (j * 137.508f + 20f) % 360f;
        int family = j % THEME_FAMILIES.length;
        float saturation = THEME_SV[family][0];
        float top = THEME_SV[family][1];
        float bottom = THEME_SV[family][2];
        String topHex = hsv(hue, saturation, top);
        String bottomHex = hsv(hue, saturation, bottom);
        for (int attempt = 0; attempt < 20 && !themeOk(topHex, bottomHex, palettes); attempt++) {
            top *= 0.88f;      // darker sky: more text contrast, more room for the balls
            bottom *= 0.9f;
            topHex = hsv(hue, saturation, top);
            bottomHex = hsv(hue, saturation, bottom);
        }
        if (!themeOk(topHex, bottomHex, palettes)) {
            throw new IllegalStateException("theme family " + j + " cannot meet the contrast rules");
        }
        String place = PLACES[j % PLACES.length];
        String name = hueName(hue) + " " + place;
        String description = THEME_FAMILIES[family] + " " + place.toLowerCase(Locale.ROOT) + ", "
            + hueName(hue).toLowerCase(Locale.ROOT) + " light";
        return Cosmetic.theme(id, unique(names, Cosmetic.Category.THEME, name), price, unlock, need,
            description, topHex, bottomHex, hsv(hue + 35f, 0.55f, 1f), hsv(hue - 70f, 0.45f, 1f));
    }

    static boolean themeOk(String top, String bottom, List<String[]> palettes) {
        if (Contrast.ratio(TEXT, top) < MIN_TEXT_CONTRAST) return false;
        if (Contrast.ratio(TEXT, bottom) < MIN_TEXT_CONTRAST) return false;
        for (String[] palette : palettes) {
            for (String colour : palette) {
                if (Contrast.distance(colour, top) < MIN_BALL_ON_BACKDROP) return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------- helpers
    private static String prefix(Cosmetic.Category category) {
        switch (category) {
            case BALL_PALETTE: return "ball.";
            case BALL_FINISH: return "finish.";
            case TUBE: return "tube.";
            default: return "theme.";
        }
    }

    private static String hueName(float hue) {
        int bucket = Math.round(((hue % 360f) + 360f) % 360f / 30f) % HUE_NAMES.length;
        return HUE_NAMES[bucket];
    }

    /** Appends II, III, ... when a category already has the name. */
    private static String unique(Set<String> names, Cosmetic.Category category, String name) {
        String candidate = name;
        String[] numerals = {" II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
        for (int i = 0; !names.add(category + candidate); i++) {
            candidate = name + (i < numerals.length ? numerals[i] : " " + (i + 2));
        }
        return candidate;
    }

    /** HSV (hue in degrees, s and v 0..1) to an upper-case RRGGBB string. */
    static String hsv(float hue, float saturation, float value) {
        float h = ((hue % 360f) + 360f) % 360f / 60f;
        float c = value * saturation;
        float x = c * (1f - Math.abs(h % 2f - 1f));
        float m = value - c;
        float r;
        float g;
        float b;
        if (h < 1f) { r = c; g = x; b = 0f; }
        else if (h < 2f) { r = x; g = c; b = 0f; }
        else if (h < 3f) { r = 0f; g = c; b = x; }
        else if (h < 4f) { r = 0f; g = x; b = c; }
        else if (h < 5f) { r = x; g = 0f; b = c; }
        else { r = c; g = 0f; b = x; }
        return String.format(Locale.ROOT, "%02X%02X%02X",
            Math.round((r + m) * 255f), Math.round((g + m) * 255f), Math.round((b + m) * 255f));
    }
}
