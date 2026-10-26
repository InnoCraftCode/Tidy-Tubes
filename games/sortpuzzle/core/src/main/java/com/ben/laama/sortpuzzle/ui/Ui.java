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

    /** Text button in one of the registered styles, with the style's standard padding. */
    public static TextButton button(Skin skin, String text, String style, Runnable onClick) {
        TextButton button = new TextButton(text, skin, style);
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

        private void apply(boolean animate) {
            Color trackColor = on ? Palette.PRIMARY : Color.valueOf("FFFFFF2A");
            Color knobColor = on ? Palette.ON_PRIMARY : Palette.TEXT_DIM;
            float knobX = on ? WIDTH - knob.getWidth() - 5f : 5f;
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
