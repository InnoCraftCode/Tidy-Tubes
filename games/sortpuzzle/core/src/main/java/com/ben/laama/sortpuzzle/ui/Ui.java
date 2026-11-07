package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

/**
 * The component kit: every reusable widget of the design system, built from skin primitives
 * so screens stay short and every button on every screen is the same button.
 *
 * <p>Sizes are in world units (720 wide). Touch targets are never below 64 units, which is
 * roughly 10 mm on a 1080p phone.</p>
 */
public final class Ui {

    /** Diameter of a round icon button. */
    public static final float ICON_BUTTON = 72f;
    /** Icon glyph size inside a round button. */
    public static final float ICON = 34f;

    private Ui() {
    }

    // ---------------------------------------------------------------- buttons
    /** Round translucent button with a white icon: back, close, settings, home. */
    public static ImageButton iconButton(Skin skin, String icon, Runnable onClick) {
        return iconButton(skin, icon, Color.valueOf("FFFFFF18"), Palette.TEXT, ICON_BUTTON, onClick);
    }

    /**
     * @param background tint of the circle
     * @param tint       tint of the glyph
     * @param size       diameter
     */
    public static ImageButton iconButton(Skin skin, String icon, Color background, Color tint,
                                         float size, Runnable onClick) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = skin.newDrawable("circle", background);
        style.down = skin.newDrawable("circle", brighten(background));
        style.disabled = skin.newDrawable("circle", Color.valueOf("FFFFFF0A"));
        style.imageUp = skin.newDrawable(icon, tint);
        style.imageDisabled = skin.newDrawable(icon, Palette.TEXT_DISABLED);
        ImageButton button = new ImageButton(style);
        button.getImageCell().size(size * ICON / ICON_BUTTON);
        button.setSize(size, size);
        click(button, onClick);
        return button;
    }

    /**
     * A text button whose caption shrinks (down to {@link #MIN_TEXT_SCALE}) when the cell gives
     * it less width than the words need, instead of spilling out of the pill and pushing its
     * card off screen. Translations vary a lot in length ("SHOP" / "BOUTIQUE" / "MAĞAZA").
     */
    public static final class FitTextButton extends TextButton {
        public FitTextButton(String text, Skin skin, String style) {
            super(text, skin, style);
        }

        /** May be squeezed: the caption scales down to fit (see {@link #layout()}). */
        @Override
        public float getMinWidth() {
            float pads = getPadLeft() + getPadRight();
            return pads + (super.getMinWidth() - pads) * MIN_TEXT_SCALE;
        }

        @Override
        public void layout() {
            Label label = getLabel();
            float available = getWidth() - getPadLeft() - getPadRight();
            float current = textScale(label);
            float natural = label.getPrefWidth() / current;
            float wanted = available <= 0f || natural <= available ? 1f
                : Math.max(MIN_TEXT_SCALE, available / natural);
            if (Math.abs(wanted - current) > 0.005f) {
                setTextScale(label, wanted); // re-lays out once; stable on the next pass
            }
            super.layout();
        }
    }

    // ------------------------------------------------------------------ text scale
    //
    // libGDX's Label.setFontScale is ABSOLUTE: while the label measures and draws, it replaces
    // the font's own scale. Our fonts are rasterised at the screen density and carry a base
    // scale of 1/density (0.67 on a 1.5x screen, ~0.36 on a 2.75x phone), so
    // label.setFontScale(0.8f) made text 2.2x BIGGER on a phone instead of 20 % smaller. Always
    // go through these two: they work relative to the font's base scale.

    /** @return the label's text scale relative to its font's normal size (1 = unchanged) */
    public static float textScale(Label label) {
        float base = label.getStyle().font.getData().scaleX;
        return label.getFontScaleX() == 1f || base == 0f ? 1f : label.getFontScaleX() / base;
    }

    /** Draws the label at {@code factor} times its font's normal size (1 = unchanged). */
    public static void setTextScale(Label label, float factor) {
        label.setFontScale(label.getStyle().font.getData().scaleX * factor);
    }

    /** The scale the label really measures and draws with (what Label applies to the font). */
    public static float effectiveFontScaleX(Label label) {
        return label.getFontScaleX() == 1f ? label.getStyle().font.getData().scaleX : label.getFontScaleX();
    }

    public static float effectiveFontScaleY(Label label) {
        return label.getFontScaleY() == 1f ? label.getStyle().font.getData().scaleY : label.getFontScaleY();
    }

    /** Smallest caption scale a squeezed button or row may use. */
    public static final float MIN_TEXT_SCALE = 0.6f;

    /** Text button in one of the registered styles, with the style's standard padding. */
    public static TextButton button(Skin skin, String text, String style, Runnable onClick) {
        TextButton button = new FitTextButton(text, skin, style);
        switch (style) {
            case "primary":
            case "secondary":
                button.pad(18f, 44f, 18f, 44f);
                break;
            default:
                button.pad(12f, 26f, 12f, 26f);
                break;
        }
        click(button, onClick);
        return button;
    }

    /**
     * A pill button with a small glyph before its text: the home navigation row. The glyph
     * takes the style's font colour so it reads as part of the label.
     */
    public static Button iconTextButton(Skin skin, String icon, String text, String style,
                                        Runnable onClick) {
        TextButton.TextButtonStyle textStyle = skin.get(style, TextButton.TextButtonStyle.class);
        Button button = new Button(textStyle);
        Image glyph = new Image(skin.newDrawable(icon, textStyle.fontColor));
        Label label = new Label(text, new Label.LabelStyle(textStyle.font, textStyle.fontColor));
        button.add(glyph).size(26f).padRight(8f);
        button.add(label);
        button.pad(12f, 20f, 12f, 22f);
        click(button, onClick);
        return button;
    }

    /** A big call-to-action with a title line and a smaller caption line. */
    public static Button twoLineButton(Skin skin, String style, String title, String caption,
                                       Runnable onClick) {
        Button button = new Button(skin.get(style, TextButton.TextButtonStyle.class));
        TextButton.TextButtonStyle textStyle = skin.get(style, TextButton.TextButtonStyle.class);
        Label big = new Label(title, new Label.LabelStyle(skin.getFont("heading"), textStyle.fontColor));
        Label small = new Label(caption, new Label.LabelStyle(skin.getFont("small"), textStyle.fontColor));
        small.getColor().a = 0.78f;
        Table stack = new Table();
        stack.add(big).row();
        stack.add(small).padTop(-4f);
        button.add(stack).pad(12f, 48f, 12f, 48f);
        click(button, onClick);
        return button;
    }

    /**
     * Scales down every label under {@code row} (at most to 75 %) until the row's preferred
     * width fits {@code maxWidth}. For rows of buttons whose words vary in length by language.
     */
    public static void shrinkTextToFit(Table row, float maxWidth) {
        java.util.List<Label> labels = new java.util.ArrayList<>();
        collectLabels(row, labels);
        float scale = 1f;
        for (int pass = 0; pass < 4 && row.getPrefWidth() > maxWidth && scale > 0.75f; pass++) {
            scale = Math.max(0.75f, scale * maxWidth / row.getPrefWidth());
            for (Label label : labels) {
                setTextScale(label, scale);
            }
            row.invalidateHierarchy();
        }
    }

    private static void collectLabels(Actor actor, java.util.List<Label> out) {
        if (actor instanceof Label) {
            out.add((Label) actor);
        } else if (actor instanceof com.badlogic.gdx.scenes.scene2d.Group) {
            for (Actor child : ((com.badlogic.gdx.scenes.scene2d.Group) actor).getChildren()) {
                collectLabels(child, out);
            }
        }
    }

    public static void click(Actor actor, Runnable onClick) {
        actor.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClick.run();
            }
        });
    }

    // ------------------------------------------------------------------ cards
    /** Translucent rounded card with the standard padding. */
    public static Table card(Skin skin) {
        Table card = new Table();
        card.setBackground(skin.newDrawable("card", Palette.SURFACE));
        card.pad(22f, 26f, 22f, 26f);
        return card;
    }

    /** Small pill with an icon and a short text: coins, boosters, level badges. */
    public static Table chip(Skin skin, String icon, Color iconTint, Label label) {
        Table chip = new Table();
        chip.setBackground(skin.newDrawable("pill", Palette.SURFACE));
        chip.pad(8f, 14f, 8f, 16f);
        if (icon != null) {
            Image image = new Image(skin.newDrawable(icon, iconTint));
            chip.add(image).size(26f).padRight(8f);
        }
        chip.add(label);
        return chip;
    }

    /** A thin translucent separator line. */
    public static Image divider(Skin skin) {
        Image line = new Image(skin.newDrawable("white", Color.valueOf("FFFFFF1A")));
        return line;
    }

    // ---------------------------------------------------------------- toggles
    /** A pill switch; the knob slides and the track tints mint when on. */
    public static final class Toggle extends Table {
        private static final float WIDTH = 78f;
        private static final float HEIGHT = 44f;
        private final Image track;
        private final Image knob;
        private boolean on;

        public Toggle(Skin skin, boolean initial, java.util.function.Consumer<Boolean> onChange) {
            on = initial;
            track = new Image(skin.newDrawable("pill", Color.WHITE));
            knob = new Image(skin.newDrawable("circle", Color.WHITE));
            setSize(WIDTH, HEIGHT);
            setTransform(false);
            track.setSize(WIDTH, HEIGHT);
            knob.setSize(HEIGHT - 10f, HEIGHT - 10f);
            addActor(track);
            addActor(knob);
            apply(false);
            setTouchable(Touchable.enabled);
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    on = !on;
                    apply(true);
                    onChange.accept(on);
                }
            });
        }

        public boolean isOn() {
            return on;
        }

        /** Moves the switch without firing the change callback (e.g. a permission was refused). */
        public void setOn(boolean value) {
            if (on == value) return;
            on = value;
            apply(true);
        }

        private void apply(boolean animate) {
            Color trackColor = on ? Palette.PRIMARY : Color.valueOf("FFFFFF2A");
            Color knobColor = on ? Palette.ON_PRIMARY : Palette.TEXT_DIM;
            float knobX = knobX();
            knob.clearActions();
            track.clearActions();
            if (animate) {
                knob.addAction(Actions.moveTo(knobX, 5f, 0.16f, Interpolation.swingOut));
                knob.addAction(Actions.color(knobColor, 0.16f));
                track.addAction(Actions.color(trackColor, 0.16f));
            } else {
                knob.setPosition(knobX, 5f);
                knob.setColor(knobColor);
                track.setColor(trackColor);
            }
        }

        /** Knob at the "end" when on: the right, or the left in a right-to-left language. */
        private float knobX() {
            boolean atRight = on != com.ben.laama.sortpuzzle.i18n.T.rtl();
            return atRight ? WIDTH - knob.getWidth() - 5f : 5f;
        }

        /** Re-asserts the resting geometry on every layout pass (rotation, RTL mirroring). */
        @Override
        public void layout() {
            super.layout();
            track.setBounds(0f, 0f, WIDTH, HEIGHT);
            if (!knob.hasActions()) {
                knob.setPosition(knobX(), 5f);
            }
        }

        @Override
        public float getPrefWidth() {
            return WIDTH;
        }

        @Override
        public float getPrefHeight() {
            return HEIGHT;
        }
    }

    // ---------------------------------------------------------------- dialogs
    /**
     * A centred card dialog with a title, ready for content and buttons. Buttons are added by
     * the caller through {@link Dialog#button(Button, Object)} so they can be styled; the
     * object given there is handed to {@code onResult} when that button is pressed, after
     * which the dialog hides itself.
     */
    public static Dialog dialog(Skin skin, String title,
                                java.util.function.Consumer<Object> onResult) {
        Dialog dialog = new Dialog("", skin, "dialog") {
            @Override
            protected void result(Object choice) {
                if (onResult != null) {
                    onResult.accept(choice);
                }
            }

            /** Arrives with a small overshoot instead of scene2d's slow linear fade. */
            private float fit = 1f;

            @Override
            public Dialog show(Stage stage) {
                setTransform(true);
                // A dialog designed for a 1280-unit portrait height must still fit a landscape
                // screen (800 units): shrink it as a whole rather than clip its buttons.
                pack();
                fit = Math.min(1f, Math.min((stage.getHeight() - 24f) / getHeight(),
                    (stage.getWidth() - 24f) / getWidth()));
                setKeepWithinStage(fit >= 1f);
                show(stage, Actions.sequence(
                    Actions.alpha(0f),
                    Actions.scaleTo(0.94f * fit, 0.94f * fit),
                    Actions.parallel(
                        Actions.fadeIn(0.18f, Interpolation.pow2Out),
                        Actions.scaleTo(fit, fit, 0.26f, Interpolation.swingOut))));
                setPosition(Math.round((stage.getWidth() - getWidth()) * 0.5f),
                    Math.round((stage.getHeight() - getHeight()) * 0.5f));
                setOrigin(Align.center);
                return this;
            }

            /** Leaves faster than it arrived (exit = two thirds of the entrance). */
            @Override
            public void hide() {
                hide(Actions.parallel(
                    Actions.fadeOut(0.14f, Interpolation.pow2In),
                    Actions.scaleTo(0.96f * fit, 0.96f * fit, 0.14f, Interpolation.pow2In)));
            }
        };
        dialog.setMovable(false);
        dialog.setModal(true);
        dialog.setKeepWithinStage(true);
        Table content = dialog.getContentTable();
        content.pad(30f, 34f, 10f, 34f);
        if (title != null && !title.isEmpty()) {
            Label label = new Label(title, skin, "title");
            label.setAlignment(Align.center);
            content.add(label).padBottom(6f).row();
        }
        dialog.getButtonTable().pad(14f, 26f, 26f, 26f).defaults().pad(0f, 7f, 0f, 7f);
        return dialog;
    }

    /** Word-wrapped body text for cards and dialogs. */
    public static Label paragraph(Skin skin, String text, String style, float width) {
        Label label = new Label(text, skin, style);
        label.setWrap(true);
        label.setAlignment(Align.center);
        label.setWidth(width);
        return label;
    }

    // ------------------------------------------------------------------ toast
    /** A short message that rises from the bottom of the screen and fades. */
    public static void toast(Stage stage, Skin skin, String message, Color color) {
        Label label = new Label(message, skin, "label");
        label.setColor(color);
        Table pill = new Table();
        pill.setBackground(skin.newDrawable("pill", Palette.CARD));
        pill.pad(12f, 24f, 12f, 24f);
        pill.add(label);
        pill.pack();
        pill.setPosition((stage.getWidth() - pill.getWidth()) * 0.5f, 170f);
        pill.setTouchable(Touchable.disabled);
        pill.getColor().a = 0f;
        stage.addActor(pill);
        pill.addAction(Actions.sequence(
            Actions.parallel(Actions.fadeIn(0.18f), Actions.moveBy(0f, 18f, 0.25f, Interpolation.pow2Out)),
            Actions.delay(1.5f),
            Actions.fadeOut(0.3f),
            Actions.removeActor()));
    }

    private static Color brighten(Color source) {
        return new Color(
            Math.min(1f, source.r + 0.08f),
            Math.min(1f, source.g + 0.08f),
            Math.min(1f, source.b + 0.08f),
            Math.min(1f, source.a + 0.1f));
    }
}
