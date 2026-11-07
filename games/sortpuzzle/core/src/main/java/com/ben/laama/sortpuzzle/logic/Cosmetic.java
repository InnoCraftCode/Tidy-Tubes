package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.i18n.T;
import com.ben.laama.sortpuzzle.GameConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The cosmetic catalogue: everything the shop can sell or progression can unlock.
 *
 * <p>Pure data, no libGDX: colours are hex strings so the rules and the tests never touch a
 * {@code Color}. Rendering reads the hex and builds whatever it needs (see {@code ui.Cosmetics}).
 * Nothing here changes a rule of the game - an item is a look, never an advantage.</p>
 *
 * <p>The catalogue is the hand-made items below (the shipped ones, ids frozen) followed by
 * {@link CatalogueGenerator}'s families, up to {@link #PER_CATEGORY} items per category. Each
 * {@link Category} has exactly one free item, listed first; it is what every player starts
 * with and what a reset falls back to.</p>
 */
public final class Cosmetic {

    /** Items per category once the generated families are added. */
    public static final int PER_CATEGORY = 100;

    /** One equipped item per category. */
    public enum Category {
        BALL_PALETTE(T.k("Balls")), BALL_FINISH(T.k("Finish")), TUBE(T.k("Tubes")), THEME(T.k("Themes"));

        private final String title;

        Category(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    /** How an item becomes owned. Only {@link #NONE} items are for sale. */
    public enum Unlock {
        /** Free (price 0) or bought with coins. */
        NONE,
        /** The daily-streak reward: owned once the streak is reached. */
        DAILY_STREAK,
        /** A star-road milestone: owned once {@link #requirement()} campaign stars are reached. */
        STARS,
        /** A world reward: owned once chapter {@link #requirement()} is cleared. */
        CHAPTER
    }

    /** Surface pattern a finish lays over its base lighting (generated finishes). */
    public enum Pattern {
        NONE(""), BANDS(T.k("Bands")), STRIPES(T.k("Stripes")), RING(T.k("Ring")), DOTS(T.k("Dots")), SWIRL(T.k("Swirl")),
        DUO(T.k("Duo")), CHECKER(T.k("Checker")), SPARKLE(T.k("Sparkle"));

        private final String title;

        Pattern(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    // --------------------------------------------------------------- ball palettes
    public static final Cosmetic BALL_CLASSIC = palette("ball.classic", T.k("Nocturne"), 0, Unlock.NONE, 0,
        T.k("The original ten colours"),
        "F2504B", "3B82F6", "34C759", "FFD23F", "A855F7",
        "FF8A3D", "14B8A6", "F472B6", "CBD5E1", "9A5B2B");
    public static final Cosmetic BALL_PASTEL = palette("ball.pastel", T.k("Daily glow"), 0, Unlock.DAILY_STREAK, 0,
        T.k("Win 3 daily puzzles in a row"),
        "FF6B6B", "4CC9F0", "80ED99", "FFD166", "B892FF",
        "F4A261", "2EC4B6", "FF8FAB", "E2E8F0", "C08457");
    public static final Cosmetic BALL_NEON = palette("ball.neon", T.k("Spectrum"), 0, Unlock.STARS,
        GameConfig.STAR_PALETTE_UNLOCK,
        T.k("Earn 30 campaign stars"),
        "F15BB5", "00BBF9", "00F5D4", "FEE440", "9B5DE5",
        "F8961E", "43AA8B", "F94144", "E9ECEF", "8D6E63");
    public static final Cosmetic BALL_COSMOS = palette("ball.cosmos", T.k("Cosmos"), 0, Unlock.STARS, 150,
        T.k("Star road: bright as a clear night"),
        "FF5C8A", "4D9DE0", "3BCEAC", "FFD93D", "8E7DFF",
        "FF9F45", "00C2D1", "F7A1C4", "DDE3EA", "A9714B");
    public static final Cosmetic BALL_CANDY = palette("ball.candy", T.k("Candy"), 250, Unlock.NONE, 0,
        T.k("Sweet, saturated tones"),
        "FF4D6D", "3D8BFF", "4CD964", "FFE14D", "9B6BFF",
        "FF9A3C", "1FD1C1", "FF7EB6", "F1F3F6", "8B5A2B");
    public static final Cosmetic BALL_OCEAN = palette("ball.ocean", T.k("Ocean"), 500, Unlock.NONE, 0,
        T.k("Coral, lagoon and sand"),
        "E8553F", "2563EB", "22C55E", "F5C542", "7C3AED",
        "FB923C", "06B6D4", "F0ABFC", "E2E8F0", "92400E");
    public static final Cosmetic BALL_SUNSET = palette("ball.sunset", T.k("Sunset"), 500, Unlock.NONE, 0,
        T.k("Warm reds to cool dusk blues"),
        "F94144", "F3722C", "F8961E", "F9C74F", "90BE6D",
        "43AA8B", "577590", "277DA1", "E07A9A", "EAD2AC");
    public static final Cosmetic BALL_BERRY = palette("ball.berry", T.k("Berry"), 500, Unlock.NONE, 0,
        T.k("Raspberry, grape and mint"),
        "E63946", "FF70A6", "C77DFF", "7B2CBF", "4CC9F0",
        "80FFDB", "FFD166", "FF9F1C", "F8F9FA", "6A994E");
    public static final Cosmetic BALL_RETRO = palette("ball.retro", T.k("Retro"), 900, Unlock.NONE, 0,
        T.k("Muted seventies tones"),
        "E76F51", "F4A261", "E9C46A", "2A9D8F", "8AB17D",
        "6D597A", "B56576", "355070", "EAE2B7", "9C6644");

    // --------------------------------------------------------------- ball finishes
    public static final Cosmetic FINISH_GLOSSY = finish("finish.glossy", T.k("Glossy"), 0, T.k("The classic shine"));
    public static final Cosmetic FINISH_MATTE = finish("finish.matte", T.k("Matte"), 250, T.k("Soft, no reflections"));
    public static final Cosmetic FINISH_GLASS = finish("finish.glass", T.k("Glass"), 500, T.k("Bright highlight, deep shadow"));
    public static final Cosmetic FINISH_GEM = finish("finish.gem", T.k("Gem"), 900, T.k("Cut facets that catch the light"));
    public static final Cosmetic FINISH_JELLY = finish("finish.jelly", T.k("Jelly"), 500, T.k("Soft, translucent, a little wobbly"));
    public static final Cosmetic FINISH_CHROME = finish("finish.chrome", T.k("Chrome"), 1500, T.k("Mirror shine with a horizon line"));

    // ---------------------------------------------------------------------- tubes
    // data: body tint (RGBA hex), rim colour (RGBA hex); rimThickness in pixels
    public static final Cosmetic TUBE_GLASS = tube("tube.glass", T.k("Glass"), 0, Unlock.NONE, 0,
        T.k("Clear glass, fine rim"), 3f, "FFFFFF14", "FFFFFF52");
    // Star-road rewards right after the free item, so the shop shows them as goals.
    public static final Cosmetic TUBE_STARLIGHT = tube("tube.starlight", T.k("Starlight"), 0, Unlock.STARS, 60,
        T.k("Star road: pale gold glow"), 3.5f, "FFE9A318", "FFE9A3F0");
    public static final Cosmetic TUBE_COMET = tube("tube.comet", T.k("Comet"), 0, Unlock.STARS, 220,
        T.k("Star road: violet tail, icy glass"), 4f, "7CC4FF1C", "B892FFF0");
    public static final Cosmetic TUBE_FROST = tube("tube.frost", T.k("Frost"), 250, Unlock.NONE, 0,
        T.k("Milky glass, soft edge"), 2.2f, "FFFFFF30", "FFFFFF66");
    public static final Cosmetic TUBE_NEON = tube("tube.neon", T.k("Neon"), 500, Unlock.NONE, 0,
        T.k("A glowing mint rim"), 4f, "FFFFFF12", "4FE3C1E6");
    public static final Cosmetic TUBE_GOLD = tube("tube.gold", T.k("Gold"), 900, Unlock.NONE, 0,
        T.k("Amber glass, golden rim"), 3.5f, "FFC85726", "FFC857F2");
    public static final Cosmetic TUBE_INK = tube("tube.ink", T.k("Ink"), 250, Unlock.NONE, 0,
        T.k("Smoked glass, bright edge"), 3f, "00000040", "FFFFFF80");
    public static final Cosmetic TUBE_ROSE = tube("tube.rose", T.k("Rose"), 500, Unlock.NONE, 0,
        T.k("Blush-tinted glass"), 3f, "F472B620", "F472B6E0");
    public static final Cosmetic TUBE_SILVER = tube("tube.silver", T.k("Silver"), 500, Unlock.NONE, 0,
        T.k("Cool steel rim"), 3f, "E2E8F020", "E2E8F0E6");
    public static final Cosmetic TUBE_CRYSTAL = tube("tube.crystal", T.k("Crystal"), 1500, Unlock.NONE, 0,
        T.k("Ice-blue glass, hairline rim"), 2.5f, "7CC4FF22", "7CC4FFE6");

    // --------------------------------------------------------------------- themes
    // data: background top, background bottom, aurora A, aurora B
    public static final Cosmetic THEME_NOCTURNE = theme("theme.nocturne", T.k("Nocturne"), 0, Unlock.NONE, 0,
        T.k("Deep indigo night"), "222B5E", "0B0E24", "3FB7C9", "7B5CE6");
    public static final Cosmetic THEME_STARFALL = theme("theme.starfall", T.k("Starfall"), 0, Unlock.STARS, 100,
        T.k("Star road: night sky, golden light"), "1B2550", "070A1C", "FFD23F", "7CC4FF");
    public static final Cosmetic THEME_NEBULA = theme("theme.nebula", T.k("Nebula"), 0, Unlock.STARS, 300,
        T.k("Star road: violet clouds, pink and mint glow"), "3A1F5E", "0D0620", "F472B6", "4FE3C1");
    public static final Cosmetic THEME_AURORA = theme("theme.aurora", T.k("Aurora"), 500, Unlock.NONE, 0,
        T.k("Plum dusk with warm light"), "5A2E6E", "1A0B24", "FF8A65", "F472B6");
    public static final Cosmetic THEME_FOREST = theme("theme.forest", T.k("Forest"), 500, Unlock.NONE, 0,
        T.k("Mossy green, cool mist"), "1E4D3A", "06150F", "5BE49B", "3FB7C9");
    public static final Cosmetic THEME_SAKURA = theme("theme.sakura", T.k("Sakura"), 900, Unlock.NONE, 0,
        T.k("Blossom pink over wine"), "6E2E4A", "24081A", "FFB3C7", "FFD23F");
    public static final Cosmetic THEME_MIDNIGHT = theme("theme.midnight", T.k("Midnight"), 250, Unlock.NONE, 0,
        T.k("Near-black, for late sessions"), "141A2E", "05070F", "4FE3C1", "3B82F6");
    public static final Cosmetic THEME_OCEAN = theme("theme.ocean", T.k("Deep sea"), 500, Unlock.NONE, 0,
        T.k("Blue depths, cold light"), "0F3A5F", "06182B", "3FB7C9", "7CC4FF");
    public static final Cosmetic THEME_LAVENDER = theme("theme.lavender", T.k("Lavender"), 500, Unlock.NONE, 0,
        T.k("Soft violet evening"), "3C2F6E", "120B2E", "B892FF", "F472B6");
    public static final Cosmetic THEME_EMBER = theme("theme.ember", T.k("Ember"), 900, Unlock.NONE, 0,
        T.k("Dark red, warm glow"), "4A1F1F", "160808", "FF8A3D", "FFC857");
    public static final Cosmetic THEME_GOLDEN = theme("theme.golden", T.k("Golden hour"), 1500, Unlock.NONE, 0,
        T.k("Bronze dusk, honey light"), "5A3A1E", "1F1208", "FFC857", "FF8A65");

    /** The hand-made items, in display order within each category. */
    private static final Cosmetic[] HANDMADE = {
        BALL_CLASSIC, BALL_PASTEL, BALL_NEON, BALL_COSMOS, BALL_CANDY, BALL_OCEAN, BALL_SUNSET,
        BALL_BERRY, BALL_RETRO,
        FINISH_GLOSSY, FINISH_MATTE, FINISH_GLASS, FINISH_GEM, FINISH_JELLY, FINISH_CHROME,
        TUBE_GLASS, TUBE_STARLIGHT, TUBE_COMET, TUBE_FROST, TUBE_NEON, TUBE_GOLD, TUBE_INK,
        TUBE_ROSE, TUBE_SILVER, TUBE_CRYSTAL,
        THEME_NOCTURNE, THEME_STARFALL, THEME_NEBULA, THEME_AURORA, THEME_FOREST, THEME_SAKURA,
        THEME_MIDNIGHT, THEME_OCEAN, THEME_LAVENDER, THEME_EMBER, THEME_GOLDEN};

    /** Every item, category by category, hand-made first then the generated families. */
    public static final List<Cosmetic> ALL = Collections.unmodifiableList(build());

    private final String id;
    private final String displayName;
    private final Category category;
    private final int price;
    private final Unlock unlock;
    private final int requirement;
    private final String description;
    private final float rimThickness;
    private final String[] hex;
    private final String finishBase;
    private final Pattern pattern;
    private final int patternCount;
    private final float patternStrength;

    Cosmetic(String id, String displayName, Category category, int price, Unlock unlock,
             int requirement, String description, float rimThickness, String finishBase,
             Pattern pattern, int patternCount, float patternStrength, String... hex) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.price = price;
        this.unlock = unlock;
        this.requirement = requirement;
        this.description = description;
        this.rimThickness = rimThickness;
        this.finishBase = finishBase;
        this.pattern = pattern;
        this.patternCount = patternCount;
        this.patternStrength = patternStrength;
        this.hex = hex;
    }

    // -------------------------------------------------------------- factories
    static Cosmetic palette(String id, String name, int price, Unlock unlock, int requirement,
                            String description, String... hex) {
        return new Cosmetic(id, name, Category.BALL_PALETTE, price, unlock, requirement,
            description, 0f, null, Pattern.NONE, 0, 0f, hex);
    }

    private static Cosmetic finish(String id, String name, int price, String description) {
        return new Cosmetic(id, name, Category.BALL_FINISH, price, Unlock.NONE, 0, description,
            0f, id, Pattern.NONE, 0, 0f);
    }

    static Cosmetic finish(String id, String name, int price, Unlock unlock, int requirement,
                           String description, String base, Pattern pattern, int count,
                           float strength) {
        return new Cosmetic(id, name, Category.BALL_FINISH, price, unlock, requirement,
            description, 0f, base, pattern, count, strength);
    }

    static Cosmetic tube(String id, String name, int price, Unlock unlock, int requirement,
                         String description, float rim, String body, String rimColour) {
        return new Cosmetic(id, name, Category.TUBE, price, unlock, requirement, description,
            rim, null, Pattern.NONE, 0, 0f, body, rimColour);
    }

    static Cosmetic theme(String id, String name, int price, Unlock unlock, int requirement,
                          String description, String top, String bottom, String lightA,
                          String lightB) {
        return new Cosmetic(id, name, Category.THEME, price, unlock, requirement, description,
            0f, null, Pattern.NONE, 0, 0f, top, bottom, lightA, lightB);
    }

    private static List<Cosmetic> build() {
        List<Cosmetic> all = new ArrayList<>();
        List<Cosmetic> handmade = java.util.Arrays.asList(HANDMADE);
        List<Cosmetic> generated = CatalogueGenerator.generate(handmade, PER_CATEGORY);
        for (Category category : Category.values()) {
            for (Cosmetic item : handmade) {
                if (item.category == category) all.add(item);
            }
            for (Cosmetic item : generated) {
                if (item.category == category) all.add(item);
            }
        }
        return all;
    }

    // ---------------------------------------------------------------- accessors
    /** Stable identifier written into the save; never rename one that has shipped. */
    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Category category() {
        return category;
    }

    /** Coin price; {@code 0} for the free default and for progression unlocks. */
    public int price() {
        return price;
    }

    public Unlock unlock() {
        return unlock;
    }

    /** Campaign stars for {@link Unlock#STARS}, chapter number for {@link Unlock#CHAPTER}, else 0. */
    public int requirement() {
        return requirement;
    }

    public String description() {
        return description;
    }

    /** @return {@code true} for the item every player owns from the start */
    public boolean isDefault() {
        return price == 0 && unlock == Unlock.NONE;
    }

    /** @return {@code true} when coins can buy it (not free, not a progression reward) */
    public boolean isForSale() {
        return price > 0 && unlock == Unlock.NONE;
    }

    /** Rim stroke in pixels; only meaningful for {@link Category#TUBE}. */
    public float rimThickness() {
        return rimThickness;
    }

    /** Finishes: the hand-made lighting model this one starts from ({@code finish.*} id). */
    public String finishBase() {
        return finishBase;
    }

    /** Finishes: the surface pattern over the base lighting. */
    public Pattern pattern() {
        return pattern;
    }

    /** Finishes: how many bands / dots / arms the pattern has. */
    public int patternCount() {
        return patternCount;
    }

    /** Finishes: how strongly the pattern darkens or lightens, 0..1. */
    public float patternStrength() {
        return patternStrength;
    }

    /** Colour data as hex strings; meaning depends on the category (see the constants). */
    public String[] hex() {
        return hex.clone();
    }

    public int hexCount() {
        return hex.length;
    }

    public String hex(int index) {
        return hex[index];
    }

    @Override
    public String toString() {
        return id;
    }

    // ------------------------------------------------------------------ lookups
    private static java.util.Map<String, Cosmetic> byIdIndex;

    /** @return the item with that id, or {@code null} for an unknown / removed id */
    public static Cosmetic byId(String id) {
        if (id == null) return null;
        java.util.Map<String, Cosmetic> index = byIdIndex;
        if (index == null) {
            index = new java.util.HashMap<>();
            for (Cosmetic cosmetic : ALL) {
                index.put(cosmetic.id, cosmetic);
            }
            byIdIndex = index;
        }
        return index.get(id);
    }

    /** @return the free item of a category, what a fresh profile has equipped */
    public static Cosmetic defaultFor(Category category) {
        for (Cosmetic cosmetic : ALL) {
            if (cosmetic.category == category && cosmetic.isDefault()) return cosmetic;
        }
        throw new IllegalStateException("no default item for " + category);
    }

    /** @return the items of a category in catalogue order */
    public static List<Cosmetic> of(Category category) {
        List<Cosmetic> items = new ArrayList<>();
        for (Cosmetic cosmetic : ALL) {
            if (cosmetic.category == category) items.add(cosmetic);
        }
        return items;
    }

    /** Maps the pre-1.1.0 integer ball palette (0..2) onto the catalogue. */
    public static Cosmetic legacyBallPalette(int index) {
        switch (index) {
            case 1:
                return BALL_PASTEL;
            case 2:
                return BALL_NEON;
            default:
                return BALL_CLASSIC;
        }
    }
}
