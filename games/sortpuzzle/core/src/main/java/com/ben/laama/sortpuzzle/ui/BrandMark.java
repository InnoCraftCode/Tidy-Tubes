package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * The logo illustration: three tubes mid-sort, built from the same tube and ball actors as the
 * board so the brand is literally the game. One ball hovers above the first tube and bobs,
 * which is the whole story of the game in a single frame.
 */
public final class BrandMark extends Group {

    private static final float BALL = 38f;
    private static final float GAP = 22f;

    public BrandMark(Skin skin, TubeTextures textures) {
        setTouchable(Touchable.disabled);
        float tubeW = TubeActor.widthFor(BALL);
        float tubeH = TubeActor.heightFor(BALL);
        float width = 3 * tubeW + 2 * GAP;
        float height = tubeH + BALL + 40f;
        setSize(width, height);

        int[][] stacks = {{1, 0, 1}, {0, 1}, {2, 2, 2, 2}};
        for (int t = 0; t < 3; t++) {
            TubeActor tube = new TubeActor(skin, textures, t, BALL);
            tube.setSize(tubeW, tubeH);
            tube.setHome(t * (tubeW + GAP), 0f);
            addActor(tube);
            for (int slot = 0; slot < stacks[t].length; slot++) {
                BallActor ball = new BallActor(skin, stacks[t][slot], BALL);
                ball.setPosition(tube.ballX(), tube.ballY(slot));
                addActor(ball);
            }
            if (t == 2) {
                // The finished tube wears a faint golden halo.
                Image halo = new Image(skin.newDrawable("glow", Palette.GOLD));
                halo.setSize(tubeW * 2.4f, tubeW * 2.4f);
                halo.setPosition(tube.getX() + tubeW * 0.5f - halo.getWidth() * 0.5f,
                    tube.getY() + tubeH * 0.5f - halo.getHeight() * 0.5f);
                halo.getColor().a = 0.35f;
                addActorBefore(tube, halo);
                halo.addAction(Actions.forever(Actions.sequence(
                    Actions.alpha(0.55f, 1.6f, Interpolation.sine),
                    Actions.alpha(0.3f, 1.6f, Interpolation.sine))));
            }
            if (t == 0) {
                // The hovering ball: lifted out of the first tube, about to travel.
                BallActor lifted = new BallActor(skin, 0, BALL);
                lifted.setPosition(tube.ballX(), tube.ballY(3) + 30f);
                addActor(lifted);
                lifted.addAction(Actions.forever(Actions.sequence(
                    Actions.moveBy(0f, 8f, 0.9f, Interpolation.sine),
                    Actions.moveBy(0f, -8f, 0.9f, Interpolation.sine))));
            }
        }
    }
}
