package com.ben.laama.sortpuzzle.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.Main;
import com.ben.laama.sortpuzzle.ui.BrandMark;

/**
 * Short branded intro: the brand mark springs in, the wordmark and tagline fade up, then the
 * home screen takes over. Total runtime ~1.7s and it can be skipped with a tap.
 */
public final class SplashScreen extends BaseScreen {

    private static final float HOLD = 1.7f;

    private boolean leaving;

    public SplashScreen(Main game) {
        super(game);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        BrandMark mark = new BrandMark(skin, game.tubes());
        Container<BrandMark> markBox = new Container<>(mark);
        markBox.setTransform(true);
        markBox.size(mark.getWidth(), mark.getHeight());
        markBox.setOrigin(Align.center);
        markBox.setScale(0.6f);
        markBox.getColor().a = 0f;
        markBox.addAction(Actions.parallel(
            Actions.fadeIn(0.3f),
            Actions.scaleTo(1f, 1f, 0.55f, Interpolation.swingOut)));

        Label title = new Label(GameConfig.APP_NAME, skin, "display");
        Label tagline = new Label(GameConfig.TAGLINE, skin, "dim");
        title.getColor().a = 0f;
        tagline.getColor().a = 0f;
        title.addAction(Actions.delay(0.35f, Actions.fadeIn(0.4f, Interpolation.fade)));
        tagline.addAction(Actions.delay(0.6f, Actions.fadeIn(0.4f, Interpolation.fade)));

        root.add(markBox).size(mark.getWidth(), mark.getHeight()).padBottom(34f).row();
        root.add(title).row();
        root.add(tagline).padTop(4f).row();

        stage.addAction(Actions.sequence(Actions.delay(HOLD), Actions.run(this::leave)));
        // Tap anywhere to skip.
        stage.getRoot().addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                leave();
                return true;
            }
        });
    }

    private void leave() {
        if (leaving) return;
        leaving = true;
        game.goHome();
    }

    @Override
    protected void onBackPressed() {
        Gdx.app.exit();
    }
}
