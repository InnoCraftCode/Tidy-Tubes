package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.Hinting;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import com.ben.laama.sortpuzzle.i18n.ArabicText;
import com.ben.laama.sortpuzzle.i18n.Language;
import com.ben.laama.sortpuzzle.i18n.T;


/**
 * Builds the entire {@link Skin} in code: the "Nocturne" design system from
 * {@code docs/design-system.md}. No atlas, no JSON; the only files are the Poppins fonts.
 *
 * <p>Everything registered on the returned skin is owned by it, so a single
 * {@code skin.dispose()} releases every generated texture and font.</p>
 *
 * <h2>Names the rest of the game relies on</h2>
 * <ul>
 *   <li>Fonts: {@code display}, {@code title}, {@code heading}, {@code body}, {@code label},
 *       {@code small}</li>
 *   <li>Drawables: {@code white}, {@code card}, {@code card-sm}, {@code card-outline},
 *       {@code pill}, {@code pill-lg}, {@code pill-outline}, {@code ball}, {@code circle},
 *       {@code glow}, {@code bg}, and every {@code icon-*} in {@link Icons#NAMES}</li>
 *   <li>Labels: {@code display}, {@code title}, {@code heading}, {@code default}, {@code label},
 *       {@code small}, {@code dim}, {@code caption}, {@code gold}, {@code accent},
 *       {@code danger}</li>
 *   <li>Buttons: {@code primary}, {@code secondary}, {@code gold}, {@code ghost},
 *       {@code danger}, {@code tile-done}, {@code tile-open}, {@code tile-current},
 *       {@code tile-locked}</li>
 *   <li>Window: {@code dialog} (also {@code default})</li>
 * </ul>
 */
public final class SkinFactory {

    /** Corner radii of the generated nine-patches, in world units. */
    private static final int CARD_RADIUS = 28;
    private static final int CARD_SMALL_RADIUS = 16;
    private static final int PILL_RADIUS = 24;
    private static final int PILL_LARGE_RADIUS = 36;
    /** Resolution of the generated ball sprite; scaled down at draw time, never up. */
    public static final int BALL_TEXTURE_SIZE = 128;

    private static final String FONT_DIR = "fonts/";
    private static final String EXTRA_CHARS = "–—…•×→←∞";

    private static float densityOverride = -1f;

    private SkinFactory() {
    }

    /**
     * Forces the font rasterisation density. The store-capture tool renders into a 1080x1920
     * frame buffer behind a smaller preview window, so it asks for 1.5 explicitly.
     *
     * @param density pixels per world unit, or a negative value to detect it from the screen
     */
    public static void setDensityOverride(float density) {
        densityOverride = density;
    }

    public static Skin create() {
        Skin skin = new Skin();

        registerFonts(skin, resolveDensity());
        registerDrawables(skin);
        registerLabelStyles(skin);
        registerButtonStyles(skin);
        registerWindowStyles(skin);
        registerScrollStyles(skin);

        return skin;
    }

    /**
     * Pixels per world unit. Fonts are rasterised at this density and scaled back down, so
     * text is crisp on a 1080p phone instead of being a 15px bitmap blown up three times.
     */
    private static float resolveDensity() {
        if (densityOverride > 0f) {
            return densityOverride;
        }
        return Responsive.fontDensity(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    // ------------------------------------------------------------------ fonts
    private static void registerFonts(Skin skin, float density) {
        FreeTypeFontGenerator.setMaxTextureSize(2048);
        String arabic = arabicCharacters();
        skin.add("display", font("Poppins-ExtraBold.ttf", "NotoSansArabic-ExtraBold.ttf", 58, density, arabic));
        skin.add("title", font("Poppins-ExtraBold.ttf", "NotoSansArabic-ExtraBold.ttf", 40, density, arabic));
        skin.add("heading", font("Poppins-Bold.ttf", "NotoSansArabic-Bold.ttf", 30, density, arabic));
        skin.add("body", font("Poppins-SemiBold.ttf", "NotoSansArabic-SemiBold.ttf", 24, density, arabic));
        skin.add("label", font("Poppins-SemiBold.ttf", "NotoSansArabic-SemiBold.ttf", 20, density, arabic));
        skin.add("small", font("Poppins-Medium.ttf", "NotoSansArabic-Medium.ttf", 17, density, arabic));
    }

    /**
     * Accented letters of the Latin-script languages (French, Spanish, Portuguese, German,
     * Indonesian, Turkish) and their quotation marks. Poppins covers Latin Extended-A.
     */
    private static final String LATIN_EXTRA =
        "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÑÒÓÔÕÖØÙÚÛÜÝŸŒßàáâãäåæçèéêëìíîïñòóôõöøùúûüýÿœ"
            + "ĞğİıŞşĆćŁłŃńŚśŹźŻżĄąĘęČčŠšŽž¡¿«»‘’‚“”„‹›·°ºª€";

    /**
     * Arabic glyphs to merge into each font: every presentation form when the game is in a
     * right-to-left language, otherwise only what the language picker needs to write
     * "العربية". Generated per skin, so a language change rebuilds the skin.
     */
    private static String arabicCharacters() {
        StringBuilder chars = new StringBuilder(".01"); // baseline references, see merge()
        if (T.rtl()) {
            for (char c = 'ﹰ'; c <= 'ﻼ'; c++) chars.append(c);
            chars.append("،؛؟٪٫٬");
        } else {
            String shaped = ArabicText.shape(Language.AR.nativeName);
            for (int i = 0; i < shaped.length(); i++) {
                if (chars.indexOf(String.valueOf(shaped.charAt(i))) < 0) chars.append(shaped.charAt(i));
            }
        }
        return chars.toString();
    }

    private static BitmapFont font(String file, String arabicFile, int worldSize, float density,
                                   String arabicChars) {
        BitmapFont latin = generate(file, worldSize, density,
            FreeTypeFontGenerator.DEFAULT_CHARS + EXTRA_CHARS + LATIN_EXTRA);
        if (!arabicChars.isEmpty()) {
            merge(latin, generate(arabicFile, worldSize, density, arabicChars));
        }
        latin.getData().setScale(1f / density);
        latin.getData().markupEnabled = true;
        latin.setUseIntegerPositions(false);
        return latin;
    }

    /**
     * Moves every glyph of {@code extra} into {@code into}: a BitmapFont has no fallback font, so
     * Arabic letters must live in the same font as the Latin ones. The glyph pages come along
     * (and are disposed with {@code into}); each glyph is shifted so both scripts share one
     * baseline: libGDX places glyphs from the cap-height line, which differs between the fonts.
     */
    private static void merge(BitmapFont into, BitmapFont extra) {
        BitmapFont.BitmapFontData target = into.getData();
        BitmapFont.BitmapFontData source = extra.getData();
        int pageOffset = into.getRegions().size;
        into.getRegions().addAll(extra.getRegions());
        // Align baselines on a glyph both fonts draw sitting on the baseline (a glyph's yoffset
        // is its bottom edge). Noto Sans Arabic has no Latin capitals, so the cap-height metrics
        // cannot be compared; the period and the digits can.
        float shift = source.capHeight - target.capHeight;
        for (char reference : new char[] {'.', '0', '1'}) {
            BitmapFont.Glyph mine = target.getGlyph(reference);
            BitmapFont.Glyph theirs = source.getGlyph(reference);
            if (mine != null && theirs != null) {
                shift = mine.yoffset - theirs.yoffset;
                break;
            }
        }
        for (BitmapFont.Glyph[] page : source.glyphs) {
            if (page == null) continue;
            for (BitmapFont.Glyph glyph : page) {
                if (glyph == null || glyph.id == ' ' || target.hasGlyph((char) glyph.id)) continue;
                glyph.page += pageOffset;
                glyph.yoffset += Math.round(shift);
                target.setGlyph(glyph.id, glyph);
            }
        }
    }

    private static BitmapFont generate(String file, int worldSize, float density, String characters) {
        FreeTypeFontGenerator generator =
            new FreeTypeFontGenerator(Gdx.files.internal(FONT_DIR + file));
        try {
            FreeTypeFontParameter parameter = new FreeTypeFontParameter();
            parameter.size = Math.round(worldSize * density);
            parameter.characters = characters;
            parameter.hinting = Hinting.AutoMedium;
            parameter.kerning = true;
            parameter.genMipMaps = true;
            parameter.minFilter = Texture.TextureFilter.MipMapLinearLinear;
            parameter.magFilter = Texture.TextureFilter.Linear;
            parameter.padTop = 2;
            parameter.padBottom = 2;
            parameter.padLeft = 2;
            parameter.padRight = 2;
            return generator.generateFont(parameter); // scaled by font() after any merge
        } finally {
            generator.dispose();
        }
    }

    // -------------------------------------------------------- cosmetic swaps
    /**
     * Replaces the shared ball sprite with the one for a finish. Actors already on screen keep
     * the drawable they were built with; the next screen picks up the new one. The texture it
     * replaces is disposed here because a replaced skin entry is no longer the skin's to free.
     *
     * @param finishId a {@code finish.*} catalogue id
     */
    public static void applyBallFinish(Skin skin, String finishId) {
        Texture previous = skin.optional("tex-ball", Texture.class);
        Texture ball = Tex.ball(BALL_TEXTURE_SIZE, finishId);
        skin.add("tex-ball", ball);
        skin.add("ball", new TextureRegionDrawable(new TextureRegion(ball)), Drawable.class);
        if (previous != null && previous != ball) {
            previous.dispose();
        }
    }

    /**
     * Re-bakes the backdrop gradient from the current {@link Palette#BG_TOP} and
     * {@link Palette#BG_BOTTOM}, after a theme change. Same disposal rule as above.
     *
     * @return the new gradient drawable, for a backdrop that is already on screen
     */
    public static Drawable rebuildBackground(Skin skin) {
        // One gradient per colour pair, kept for the skin's lifetime and never disposed here:
        // a screen still fading out (or one built before the previous screen's hide) may be
        // drawing the previous gradient, and a disposed texture renders black. A gradient is a
        // 256-pixel strip and there are only a handful of themes, so the cache stays tiny.
        String key = "tex-bg-" + Palette.BG_TOP + "-" + Palette.BG_BOTTOM;
        Texture background = skin.optional(key, Texture.class);
        if (background == null) {
            background = Tex.verticalGradient(256, Palette.BG_TOP, Palette.BG_BOTTOM);
            background.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            skin.add(key, background);
        }
        Drawable drawable = new TextureRegionDrawable(new TextureRegion(background));
        skin.add("bg", drawable, Drawable.class);
        return drawable;
    }

    // -------------------------------------------------------------- drawables
    private static void registerDrawables(Skin skin) {
        Texture white = Tex.white();
        skin.add("tex-white", white);
        skin.add("white", new TextureRegionDrawable(new TextureRegion(white)), Drawable.class);

        skin.add("card", ninePatch(skin, "tex-card", Tex.roundRect(CARD_RADIUS), CARD_RADIUS),
            Drawable.class);
        skin.add("card-sm",
            ninePatch(skin, "tex-card-sm", Tex.roundRect(CARD_SMALL_RADIUS), CARD_SMALL_RADIUS),
            Drawable.class);
        skin.add("card-outline",
            ninePatch(skin, "tex-card-outline", Tex.roundRectOutline(CARD_RADIUS, 3f),
                CARD_RADIUS), Drawable.class);
        skin.add("pill", ninePatch(skin, "tex-pill", Tex.roundRect(PILL_RADIUS), PILL_RADIUS),
            Drawable.class);
        skin.add("pill-lg",
            ninePatch(skin, "tex-pill-lg", Tex.roundRect(PILL_LARGE_RADIUS), PILL_LARGE_RADIUS),
            Drawable.class);
        skin.add("pill-outline",
            ninePatch(skin, "tex-pill-outline", Tex.roundRectOutline(PILL_RADIUS, 3f),
                PILL_RADIUS), Drawable.class);

        Texture ball = Tex.shadedBall(BALL_TEXTURE_SIZE, 0.6f);
        skin.add("tex-ball", ball);
        skin.add("ball", new TextureRegionDrawable(new TextureRegion(ball)), Drawable.class);

        Texture circle = Tex.circle(128);
        skin.add("tex-circle", circle);
        skin.add("circle", new TextureRegionDrawable(new TextureRegion(circle)), Drawable.class);

        Texture glow = Tex.radialGlow(160, 2.2f);
        skin.add("tex-glow", glow);
        skin.add("glow", new TextureRegionDrawable(new TextureRegion(glow)), Drawable.class);

        Texture background = Tex.verticalGradient(256, Palette.BG_TOP, Palette.BG_BOTTOM);
        background.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        skin.add("tex-bg", background);
        skin.add("bg", new TextureRegionDrawable(new TextureRegion(background)), Drawable.class);

        for (String name : Icons.NAMES) {
            Texture icon = Icons.render(name);
            skin.add("tex-" + name, icon);
            TextureRegion region = new TextureRegion(icon);
            if (T.rtl() && ("icon-back".equals(name) || "icon-next".equals(name))) {
                region.flip(true, false); // "back" points right in a right-to-left layout
            }
            skin.add(name, new TextureRegionDrawable(region), Drawable.class);
        }

        for (String code : Flags.CODES) {
            Texture flag = Flags.render(code);
            skin.add("tex-flag-" + code, flag);
            skin.add("flag-" + code, new TextureRegionDrawable(new TextureRegion(flag)), Drawable.class);
        }
    }

    private static NinePatchDrawable ninePatch(Skin skin, String texName, Texture texture,
                                               int radius) {
        skin.add(texName, texture);
        int split = radius + 1;
        NinePatch patch = new NinePatch(new TextureRegion(texture), split, split, split, split);
        return new NinePatchDrawable(patch);
    }

    // ------------------------------------------------------------------ labels
    private static void registerLabelStyles(Skin skin) {
        skin.add("display", new Label.LabelStyle(skin.getFont("display"), Palette.TEXT));
        skin.add("title", new Label.LabelStyle(skin.getFont("title"), Palette.TEXT));
        skin.add("heading", new Label.LabelStyle(skin.getFont("heading"), Palette.TEXT));
        skin.add("default", new Label.LabelStyle(skin.getFont("body"), Palette.TEXT));
        skin.add("label", new Label.LabelStyle(skin.getFont("label"), Palette.TEXT));
        skin.add("small", new Label.LabelStyle(skin.getFont("small"), Palette.TEXT));
        skin.add("dim", new Label.LabelStyle(skin.getFont("label"), Palette.TEXT_DIM));
        skin.add("caption", new Label.LabelStyle(skin.getFont("small"), Palette.TEXT_DIM));
        skin.add("gold", new Label.LabelStyle(skin.getFont("label"), Palette.GOLD));
        skin.add("accent", new Label.LabelStyle(skin.getFont("label"), Palette.PRIMARY));
        skin.add("danger", new Label.LabelStyle(skin.getFont("label"), Palette.DANGER));
    }

    // ----------------------------------------------------------------- buttons
    private static void registerButtonStyles(Skin skin) {
        skin.add("primary", buttonStyle(skin, "pill-lg",
            Palette.PRIMARY, Palette.PRIMARY_DOWN, Palette.ON_PRIMARY, "heading"));
        skin.add("secondary", buttonStyle(skin, "pill-lg",
            Palette.SURFACE_STRONG, Color.valueOf("FFFFFF40"), Palette.TEXT, "heading"));
        skin.add("gold", buttonStyle(skin, "pill",
            Palette.GOLD, Palette.GOLD_DOWN, Palette.ON_GOLD, "label"));
        skin.add("ghost", buttonStyle(skin, "pill",
            Color.valueOf("FFFFFF14"), Color.valueOf("FFFFFF30"), Palette.TEXT, "label"));
        skin.add("danger", buttonStyle(skin, "pill",
            Color.valueOf("FF6F7D22"), Color.valueOf("FF6F7D44"), Palette.DANGER, "label"));

        // Level-select tiles.
        skin.add("tile-done", buttonStyle(skin, "card-sm",
            Color.valueOf("2A8C6A"), Color.valueOf("216E54"), Palette.TEXT, "heading"));
        skin.add("tile-open", buttonStyle(skin, "card-sm",
            Palette.SURFACE_STRONG, Color.valueOf("FFFFFF40"), Palette.TEXT, "heading"));
        skin.add("tile-current", buttonStyle(skin, "card-sm",
            Palette.PRIMARY, Palette.PRIMARY_DOWN, Palette.ON_PRIMARY, "heading"));
        skin.add("tile-locked", buttonStyle(skin, "card-sm",
            Color.valueOf("FFFFFF0C"), Color.valueOf("FFFFFF0C"), Palette.TEXT_DISABLED, "heading"));
    }

    private static TextButton.TextButtonStyle buttonStyle(Skin skin, String shape, Color up,
                                                          Color down, Color fontColor,
                                                          String fontName) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = skin.getFont(fontName);
        style.fontColor = fontColor;
        style.downFontColor = fontColor;
        style.disabledFontColor = Palette.TEXT_DISABLED;
        style.up = skin.newDrawable(shape, up);
        style.down = skin.newDrawable(shape, down);
        style.over = skin.newDrawable(shape, lighten(up, 0.06f));
        style.disabled = skin.newDrawable(shape, Color.valueOf("FFFFFF0C"));
        style.pressedOffsetY = -2f;
        return style;
    }

    private static Color lighten(Color source, float amount) {
        return new Color(
            Math.min(1f, source.r + amount),
            Math.min(1f, source.g + amount),
            Math.min(1f, source.b + amount),
            source.a);
    }

    // ------------------------------------------------------------------ window
    private static void registerWindowStyles(Skin skin) {
        Window.WindowStyle dialog = new Window.WindowStyle();
        dialog.titleFont = skin.getFont("heading");
        dialog.titleFontColor = Palette.TEXT;
        dialog.background = skin.newDrawable("card", Palette.CARD);
        dialog.stageBackground = skin.newDrawable("white", Palette.SCRIM);
        skin.add("dialog", dialog);
        skin.add("default", dialog);
    }

    // ------------------------------------------------------------------ scroll
    private static void registerScrollStyles(Skin skin) {
        ScrollPane.ScrollPaneStyle scroll = new ScrollPane.ScrollPaneStyle();
        scroll.vScroll = skin.newDrawable("pill", Color.valueOf("FFFFFF0C"));
        scroll.vScrollKnob = skin.newDrawable("pill", Color.valueOf("FFFFFF36"));
        skin.add("default", scroll);
    }
}
