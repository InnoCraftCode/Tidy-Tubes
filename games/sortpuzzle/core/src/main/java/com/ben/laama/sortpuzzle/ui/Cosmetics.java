package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import com.ben.laama.sortpuzzle.logic.Cosmetic;
import com.ben.laama.sortpuzzle.logic.Shop;
import com.ben.laama.sortpuzzle.save.SaveData;

/**
 * Turns the equipped catalogue items into what the renderer uses: the shared colours in
 * {@link Palette}, the ball sprite in the {@link Skin}, the rim width in {@link TubeTextures}
 * and the baked backdrop gradient.
 *
 * <p>This is the one place that knows which item touches which resource. Screens call
 * {@link #apply} after a purchase, an equip or a reset, and the shop calls {@link #applyItem}
 * to preview one item on top of the equipped set. Texture work happens here only, never per
 * frame: equipping an item costs one small rasterisation.</p>
 */
public final class Cosmetics {

    private Cosmetics() {
    }

    /**
     * Applies everything the save says is equipped (falling back to the free items for
     * anything unowned or unknown).
     *
     * @return the rebuilt backdrop gradient, for a backdrop that is already on screen
     */
    public static Drawable apply(Skin skin, TubeTextures tubes, SaveData save) {
        applyItem(skin, tubes, Shop.equipped(save, Cosmetic.Category.BALL_PALETTE));
        applyItem(skin, tubes, Shop.equipped(save, Cosmetic.Category.BALL_FINISH));
        applyItem(skin, tubes, Shop.equipped(save, Cosmetic.Category.TUBE));
        return applyItem(skin, tubes, Shop.equipped(save, Cosmetic.Category.THEME));
    }

    /**
     * Applies one item to the renderer, whatever the save says: the shop's live preview.
     *
     * @return the rebuilt backdrop gradient for a theme, {@code null} for anything else
     */
    public static Drawable applyItem(Skin skin, TubeTextures tubes, Cosmetic item) {
        switch (item.category()) {
            case BALL_PALETTE:
                Palette.applyBallPalette(item);
                return null;
            case BALL_FINISH:
                // Texture work is skipped when nothing changes: a backdrop or a board already
                // on screen may hold the current sprite, and must not see it disposed.
                if (!item.id().equals(appliedFinish)) {
                    SkinFactory.applyBallFinish(skin, item.id());
                    appliedFinish = item.id();
                }
                return null;
            case TUBE:
                Palette.applyTubeStyle(item);
                tubes.setRimThickness(item.rimThickness());
                return null;
            default:
                if (item == Palette.theme() && skin.optional("bg", Drawable.class) != null) {
                    return skin.getDrawable("bg");
                }
                Palette.applyTheme(item);
                return SkinFactory.rebuildBackground(skin);
        }
    }

    /** The finish whose sprite the skin currently holds; {@link SkinFactory#create} starts glossy. */
    private static String appliedFinish = Cosmetic.FINISH_GLOSSY.id();
}
