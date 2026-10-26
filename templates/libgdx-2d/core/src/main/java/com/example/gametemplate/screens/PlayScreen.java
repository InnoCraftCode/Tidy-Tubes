package com.example.gametemplate.screens;

import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import com.example.gametemplate.GameConfig;
import com.example.gametemplate.TemplateGame;
import com.example.gametemplate.ads.AdProvider;
import com.example.gametemplate.logic.RoundState;
import com.example.gametemplate.logic.Target;

/**
 * The only screen: a 30-second tap-the-targets round, then a game-over panel.
 *
 * <p>Ad placements, all at natural breaks and all opt-in or paced:</p>
 * <ul>
 *   <li>Rewarded "+10s" button on the game-over panel — the player chooses to watch.</li>
 *   <li>Interstitial between rounds when {@code AdPacing} allows — after the player taps "Play
 *       again", never mid-round and never over a button the player is reaching for.</li>
 * </ul>
 * Rules live in {@link RoundState}; this class draws and routes input only.
 */
public final class PlayScreen extends ScreenAdapter {

    private static final Color BG = new Color(0x1d2433ff);
    private static final Color TARGET = new Color(0xffb703ff);
    private static final Color PANEL = new Color(0x000000b0);
    private static final Color BUTTON = new Color(0x219ebcff);
    private static final Color BUTTON_ALT = new Color(0x8ecae6ff);

    private final TemplateGame game;
    private final FitViewport viewport = new FitViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);
    private final GlyphLayout layout = new GlyphLayout();
    private final Vector2 touch = new Vector2();
    private final Random random = new Random();

    private final Rectangle playAgainButton = new Rectangle(160, 520, 400, 110);
    private final Rectangle continueButton = new Rectangle(160, 680, 400, 110);
    private final Rectangle privacyButton = new Rectangle(210, 380, 300, 80);

    private RoundState round;
    private boolean roundEndHandled;
    private boolean interstitialDue;
    /** True while a full-screen ad owns the screen; input is ignored until its callback lands. */
    private boolean waitingForAd;

    public PlayScreen(TemplateGame game) {
        this.game = game;
        newRound();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                viewport.unproject(touch.set(screenX, screenY));
                onTap(touch.x, touch.y);
                return true;
            }

            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
                    Gdx.app.exit();
                    return true;
                }
                return false;
            }
        });
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
    }

    private void newRound() {
        round = new RoundState(random, GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);
        roundEndHandled = false;
    }

    private void onTap(float x, float y) {
        if (waitingForAd) {
            return;
        }
        if (!round.isOver()) {
            round.tap(x, y);
            return;
        }
        AdProvider ads = game.ads();
        if (playAgainButton.contains(x, y)) {
            playAgain();
        } else if (continueVisible() && continueButton.contains(x, y)) {
            waitingForAd = true;
            ads.showRewarded(new AdProvider.RewardListener() {
                @Override
                public void onRewarded() {
                    waitingForAd = false;
                    game.adPacing().onFullScreenAdShown();
                    round.grantContinue();
                    roundEndHandled = false;
                }

                @Override
                public void onFailed() {
                    waitingForAd = false;
                }
            });
        } else if (ads.isPrivacyOptionsRequired() && privacyButton.contains(x, y)) {
            ads.showPrivacyOptions();
        }
    }

    private void playAgain() {
        AdProvider ads = game.ads();
        if (interstitialDue && ads.isInterstitialReady()) {
            interstitialDue = false;
            waitingForAd = true;
            ads.showInterstitial(() -> {
                waitingForAd = false;
                game.adPacing().onFullScreenAdShown();
                newRound();
            });
        } else {
            newRound();
        }
    }

    private boolean continueVisible() {
        return round.canContinue() && game.ads().isRewardedReady();
    }

    @Override
    public void render(float delta) {
        round.update(Math.min(delta, 1 / 20f)); // clamp so a hitch cannot skip half a target's life
        if (round.isOver() && !roundEndHandled) {
            roundEndHandled = true;
            game.submitScore(round.score());
            interstitialDue |= game.adPacing().onBreak();
        }

        ScreenUtils.clear(BG);
        viewport.apply();
        ShapeRenderer shapes = game.shapes();
        shapes.setProjectionMatrix(viewport.getCamera().combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(TARGET);
        for (Target t : round.targets()) {
            shapes.circle(t.x, t.y, t.radius * (0.4f + 0.6f * t.remaining()), 48);
        }
        if (round.isOver()) {
            shapes.setColor(PANEL);
            shapes.rect(0, 0, GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);
            fill(shapes, playAgainButton, BUTTON);
            if (continueVisible()) {
                fill(shapes, continueButton, BUTTON_ALT);
            }
            if (game.ads().isPrivacyOptionsRequired()) {
                fill(shapes, privacyButton, Color.DARK_GRAY);
            }
        }
        shapes.end();

        game.batch().setProjectionMatrix(viewport.getCamera().combined);
        game.batch().begin();
        float top = GameConfig.WORLD_HEIGHT - 50;
        text("Score " + round.score(), 40, top, 3f, false);
        text(String.format("%.1fs", round.timeLeft()), GameConfig.WORLD_WIDTH - 40, top, 3f, true);
        if (round.isOver()) {
            text("Time!", GameConfig.WORLD_WIDTH / 2, 1050, 5f, null);
            text("Score " + round.score() + "   Best " + game.bestScore(), GameConfig.WORLD_WIDTH / 2, 930, 2.6f, null);
            label(playAgainButton, "Play again");
            if (continueVisible()) {
                label(continueButton, "+" + (int) GameConfig.REWARD_EXTRA_SECONDS + "s  (watch ad)");
            }
            if (game.ads().isPrivacyOptionsRequired()) {
                label(privacyButton, "Privacy options");
            }
        }
        game.batch().end();
    }

    private static void fill(ShapeRenderer shapes, Rectangle r, Color c) {
        shapes.setColor(c);
        shapes.rect(r.x, r.y, r.width, r.height);
    }

    private void label(Rectangle r, String s) {
        text(s, r.x + r.width / 2, r.y + r.height / 2 + 14, 2.4f, null);
    }

    /** @param alignRight {@code null} centres the text on x. */
    private void text(String s, float x, float y, float scale, Boolean alignRight) {
        BitmapFont font = game.font();
        font.getData().setScale(scale);
        layout.setText(font, s);
        float drawX = alignRight == null ? x - layout.width / 2 : alignRight ? x - layout.width : x;
        font.draw(game.batch(), layout, drawX, y);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }
}
