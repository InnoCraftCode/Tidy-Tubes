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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import com.ben.laama.sortpuzzle.GameConfig;

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
        float byWidth = Gdx.graphics.getWidth() / GameConfig.WORLD_WIDTH;
        float byHeight = Gdx.graphics.getHeight() / GameConfig.WORLD_HEIGHT;
        return MathUtils.clamp(Math.min(byWidth, byHeight), 1f, 2.5f);
    }

    // ------------------------------------------------------------------ fonts
    private static void registerFonts(Skin skin, float density) {
        FreeTypeFontGenerator.setMaxTextureSize(2048);
        skin.add("display", font("Poppins-ExtraBold.ttf", 58, density));
        skin.add("title", font("Poppins-ExtraBold.ttf", 40, density));
        skin.add("heading", font("Poppins-Bold.ttf", 30, density));
        skin.add("body", font("Poppins-SemiBold.ttf", 24, density));
        skin.add("label", font("Poppins-SemiBold.ttf", 20, density));
        skin.add("small", font("Poppins-Medium.ttf", 17, density));
    }

    private static BitmapFont font(String file, int worldSize, float density) {
        FreeTypeFontGenerator generator =
            new FreeTypeFontGenerator(Gdx.files.internal(FONT_DIR + file));
        try {
            FreeTypeFontParameter parameter = new FreeTypeFontParameter();
            parameter.size = Math.round(worldSize * density);
            parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + EXTRA_CHARS;
            parameter.hinting = Hinting.AutoMedium;
            parameter.kerning = true;
            parameter.genMipMaps = true;
            parameter.minFilter = Texture.TextureFilter.MipMapLinearLinear;
            parameter.magFilter = Texture.TextureFilter.Linear;
            parameter.padTop = 2;
            parameter.padBottom = 2;
            parameter.padLeft = 2;
            parameter.padRight = 2;
            BitmapFont font = generator.generateFont(parameter);
            font.getData().setScale(1f / density);
            font.getData().markupEnabled = true;
            font.setUseIntegerPositions(false);
            return font;
        } finally {
            generator.dispose();
        }
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
            skin.add(name, new TextureRegionDrawable(new TextureRegion(icon)), Drawable.class);
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
