package com.ben.laama.sortpuzzle.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import com.ben.laama.sortpuzzle.logic.LevelNumbers;
import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.Main;
import com.ben.laama.sortpuzzle.i18n.Names;
import com.ben.laama.sortpuzzle.i18n.T;
import com.ben.laama.sortpuzzle.ads.AdProvider;
import com.ben.laama.sortpuzzle.audio.SoundManager;
import com.ben.laama.sortpuzzle.logic.Difficulty;
import com.ben.laama.sortpuzzle.logic.DailyChallenge;
import com.ben.laama.sortpuzzle.logic.DropTarget;
import com.ben.laama.sortpuzzle.logic.ChallengeRules;
import com.ben.laama.sortpuzzle.logic.Hint;
import com.ben.laama.sortpuzzle.logic.HintEngine;
import com.ben.laama.sortpuzzle.logic.Level;
import com.ben.laama.sortpuzzle.logic.LevelGenerator;
import com.ben.laama.sortpuzzle.logic.LevelRating;
import com.ben.laama.sortpuzzle.logic.MoveValidator;
import com.ben.laama.sortpuzzle.logic.RewardOffer;
import com.ben.laama.sortpuzzle.logic.Solver;
import com.ben.laama.sortpuzzle.logic.LeaderboardScores;
import com.ben.laama.sortpuzzle.logic.WinChecker;
import com.ben.laama.sortpuzzle.logic.Breathing;
import com.ben.laama.sortpuzzle.logic.Cosmetic;
import com.ben.laama.sortpuzzle.logic.Shop;
import com.ben.laama.sortpuzzle.logic.StarRoad;
import com.ben.laama.sortpuzzle.logic.WinSummary;
import com.ben.laama.sortpuzzle.logic.World;
import com.ben.laama.sortpuzzle.logic.Assists;
import com.ben.laama.sortpuzzle.ui.BreatheOverlay;
import com.ben.laama.sortpuzzle.ui.Ripple;
import com.ben.laama.sortpuzzle.ui.ShareCard;
import com.ben.laama.sortpuzzle.services.AnalyticsEvents;
import com.ben.laama.sortpuzzle.services.GameServices;
import com.ben.laama.sortpuzzle.model.BoardState;
import com.ben.laama.sortpuzzle.model.Move;
import com.ben.laama.sortpuzzle.model.Packed;
import com.ben.laama.sortpuzzle.save.SaveData;
import com.ben.laama.sortpuzzle.ui.ArcMoveAction;
import com.ben.laama.sortpuzzle.ui.BallActor;
import com.ben.laama.sortpuzzle.ui.BoardLayout;
import com.ben.laama.sortpuzzle.ui.HudTable;
import com.ben.laama.sortpuzzle.ui.Palette;
import com.ben.laama.sortpuzzle.ui.TubeActor;
import com.ben.laama.sortpuzzle.ui.Ui;
import com.ben.laama.sortpuzzle.ui.WinBurst;

import java.time.LocalDate;

/**
 * The board itself: layout, input, animation, hints and the win/lose flow.
 *
 * <p>The model ({@link BoardState}) is updated the instant a move is accepted; the actors then
 * animate towards wherever the model says they belong. Keeping those two apart is what makes
 * fast input feel right — a player can start the next move before the previous balls have
 * landed, and nothing desynchronises, because every ball is simply re-aimed at its new slot.</p>
 *
 * <p>Everything expensive runs on a worker thread: level generation, the par search that sets
 * the move limit, and every hint. Generating a ten-colour board is single-digit milliseconds on
 * a desktop but can take a couple of hundred on a phone, and the searches longer still.</p>
 *
 * <p>Rewards (hint unlock, extra moves) go through {@link RewardOffer}: a rewarded video when
 * the ad provider has one loaded, coins otherwise, an honest message when neither is possible.
 * Interstitials are confined to {@link #afterBreak}.</p>
 */
public final class GameScreen extends BaseScreen implements HudTable.Listener {

    private enum Phase {
        LOADING, PLAYING, WON, OUT_OF_MOVES
    }

    private static final float BOARD_MARGIN_X = 36f;
    /** Clear space between the HUD chrome and the board (room for the status pill's pop). */
    private static final float HUD_BOARD_GAP = 16f;
    private static final float TUBE_GAP = 16f;
    private static final float ROW_GAP = 70f;
    private static final float MAX_TUBE_WIDTH = 108f;
    private static final float DRAG_THRESHOLD = 18f;
    private static final int MAX_TUBES_PER_ROW = 7;
    /** Two rows of seven. Past this the tubes get too narrow to tap reliably. */
    private static final int MAX_TUBES = 14;

    private final int levelIndex;
    private final boolean dailyChallenge;
    private final String dailyDate;
    private final long dailySeed;
    private final ChallengeRules challengeRules;
    private final boolean challengeRun;

    private final Group tubeGroup = new Group();
    private final Group ballGroup = new Group();
    private final Array<TubeActor> tubes = new Array<>();
    private final Array<Array<BallActor>> stacks = new Array<>();
    private final Array<BoardState> stateHistory = new Array<>();
    private final Array<Move> moveHistory = new Array<>();

    private final HudTable hud;
    private final WinBurst burst = new WinBurst();
    private final Table loadingHolder;

    private Level level;
    private BoardState state;
    private Phase phase = Phase.LOADING;
    private int selected = -1;
    private int moves;
    private int parMoves = Level.PAR_UNKNOWN;
    private int moveLimit;
    private int freeBoosters = GameConfig.FREE_BOOSTERS_PER_LEVEL;
    private int freeUndos = Assists.UNDO.freePerLevel();
    private int freeResets = Assists.RESET.freePerLevel();
    private float ballSize = 64f;
    private boolean disposed;
    /** Star-road and chapter skins this win unlocked, announced on the result card (campaign only). */
    private java.util.List<Cosmetic> starUnlocks = java.util.Collections.emptyList();
    private int dragPointer = -1;
    private boolean dragMoved;
    private float dragDownX;
    private float dragDownY;

    // ---- hints ----
    private int freeHints = GameConfig.FREE_HINTS_PER_LEVEL;
    private boolean hintsUnlocked;
    private boolean hintPending;
    private int hintRequestId;
    private int suggestedFrom = -1;
    private int suggestedTo = -1;
    private Actor hintGhost;
    private int dragTube = -1;
    private float dragStartX;
    private float dragStartY;

    public GameScreen(Main game, int levelIndex) {
        this(game, levelIndex, null, ChallengeRules.CLASSIC, false);
    }

    public GameScreen(Main game, LocalDate dailyDate) {
        this(game, DailyChallenge.levelFor(dailyDate), dailyDate, ChallengeRules.CLASSIC, false);
    }

    public GameScreen(Main game, int levelIndex, ChallengeRules rules) {
        this(game, levelIndex, null, rules, true);
    }

    private GameScreen(Main game, int levelIndex, LocalDate dailyDate,
                       ChallengeRules rules, boolean challengeRun) {
        super(game);
        this.levelIndex = levelIndex;
        this.dailyChallenge = dailyDate != null;
        this.dailyDate = dailyChallenge ? DailyChallenge.dateKey(dailyDate) : "";
        if (dailyChallenge && this.dailyDate.equals(DailyChallenge.dateKey(DailyChallenge.todayUtc()))) {
            // Lets tonight's reminder say "almost there" instead of "ready" if it is not finished.
            game.save().markDailyStarted(this.dailyDate);
        }
        this.dailySeed = dailyChallenge ? DailyChallenge.seedFor(dailyDate) : 0L;
        this.challengeRules = rules == null ? ChallengeRules.CLASSIC : rules;
        this.challengeRun = challengeRun && !this.challengeRules.isClassic();

        background.setIntensity(0.35f); // the board needs the attention, not the backdrop
        ballGroup.setTouchable(Touchable.disabled);
        stage.addActor(tubeGroup);
        stage.addActor(ballGroup);
        safeArea.toFront(); // the HUD (inside the safe area) draws above the board

        hud = new HudTable(skin, this);
        String title = dailyChallenge ? T.r("DAILY CHALLENGE")
            : this.challengeRun ? T.r("CUSTOM CHALLENGE") : T.f("Level {0}", LevelNumbers.format(levelIndex));
        String subtitle = this.challengeRun
            ? T.f("{0}  •  {1} colours", this.challengeRules.summary(), Difficulty.colorsFor(levelIndex))
            : T.f("{0}  •  {1} colours", T.raw(Difficulty.bandName(levelIndex)), Difficulty.colorsFor(levelIndex));
        hud.setLevel(title, subtitle);
        hud.setMoves(0, 0);
        hud.showPar(Level.PAR_UNKNOWN);
        hud.setAudio(game.sound().areEffectsOn(), game.sound().isMusicOn());
        hud.setCoins(game.save().coins);
        hud.setUndoEnabled(false);
        refreshStock();
        hud.setHintCaption(hintCaption());
        hud.setHintEnabled(false);
        hud.setLandscape(isLandscape());
        safeArea.addActor(hud);

        loadingHolder = buildLoadingIndicator();
        stage.addActor(loadingHolder);

        stage.addActor(burst); // last, so its ShapeRenderer pass sits on top
        installTubeInput();

        startWorker();
    }

    public int levelIndex() {
        return levelIndex;
    }

    /** "Shuffling…" with three balls breathing, shown while the worker builds the board. */
    private Table buildLoadingIndicator() {
        Table holder = new Table();
        holder.setFillParent(true);
        holder.setTouchable(Touchable.disabled);
        Table balls = new Table();
        for (int i = 0; i < 3; i++) {
            Image ball = new Image(skin.getDrawable("ball"));
            ball.setColor(Palette.ball(i));
            ball.setOrigin(Align.center);
            balls.add(ball).size(30f).pad(0f, 7f, 0f, 7f);
            ball.addAction(Actions.forever(Actions.sequence(
                Actions.delay(i * 0.15f),
                Actions.moveBy(0f, 12f, 0.3f, Interpolation.sineOut),
                Actions.moveBy(0f, -12f, 0.3f, Interpolation.sineIn),
                Actions.delay(0.4f - i * 0.15f))));
        }
        holder.add(balls).row();
        holder.add(new Label(T.r("Shuffling the tubes…"), skin, "dim")).padTop(16f);
        return holder;
    }

    // ------------------------------------------------------- scripted play (captures)
    /** @return whether the board is dealt and accepting moves */
    public boolean isReady() {
        return phase == Phase.PLAYING;
    }

    public boolean isWon() {
        return phase == Phase.WON;
    }

    /**
     * Plays the solver's solution from the current position, one move every {@code interval}
     * seconds, through the same tap path a player uses - so animations and sounds are real.
     * Used by the desktop store-capture tool to film screenshots and the promo video.
     *
     * @param maxMoves stop after this many moves (the whole solution if larger)
     * @return the number of moves scheduled, 0 if the board is not ready or no solution was found
     */
    public int autoPlay(float interval, int maxMoves) {
        if (phase != Phase.PLAYING) {
            return 0;
        }
        Solver.Result result = Solver.findOptimal(state, 2_000_000L);
        if (!result.solved()) {
            result = Solver.findAny(state, 2_000_000L);
        }
        if (!result.solved()) {
            return 0;
        }
        int count = Math.min(maxMoves, result.moveCount());
        SequenceAction script = Actions.sequence();
        for (int i = 0; i < count; i++) {
            final Move move = result.moves().get(i);
            script.addAction(Actions.sequence(
                Actions.run(() -> onTubeTapped(move.from())),
                Actions.delay(interval * 0.45f),
                Actions.run(() -> onTubeTapped(move.to())),
                Actions.delay(interval * 0.55f)));
        }
        stage.addAction(script);
        return count;
    }

    // ------------------------------------------------------------------ loading
    /**
     * Generates the board, then searches for its optimal length, handing each result back to
     * the render thread as it arrives so the board appears without waiting for par.
     */
    private void startWorker() {
        Thread worker = new Thread(() -> {
            Level generated = dailyChallenge
                ? LevelGenerator.generate(levelIndex, dailySeed)
                : LevelGenerator.generate(levelIndex);
            Gdx.app.postRunnable(() -> onLevelReady(generated));

            Solver.Result optimal = Solver.findOptimal(generated.start(),
                GameConfig.PAR_SEARCH_NODE_BUDGET);
            int par = optimal.solved() ? optimal.moveCount() : Level.PAR_UNKNOWN;
            if (par == Level.PAR_UNKNOWN) {
                Solver.Result any = Solver.findAny(generated.start(),
                    GameConfig.ANY_SOLUTION_NODE_BUDGET);
                par = any.solved() ? any.moveCount() : Level.PAR_UNKNOWN;
            }
            int resolved = par;
            Gdx.app.postRunnable(() -> onParReady(resolved));
        }, "level-" + levelIndex);
        worker.setDaemon(true);
        worker.start();
    }

    private void onLevelReady(Level generated) {
        if (disposed) {
            return;
        }
        if (!shown) {
            // The worker can finish while the previous screen is still fading out. Dealing the
            // board before show()/resize() would let the first relayout cut the deal-in short
            // (clearActions) and strand every ball at alpha 0, so wait for the first frame.
            pendingLevel = generated;
            return;
        }
        level = generated;
        state = generated.start();
        phase = Phase.PLAYING;
        game.analytics().event(AnalyticsEvents.LEVEL_START,
            AnalyticsEvents.P_LEVEL, levelIndex, AnalyticsEvents.P_MODE, analyticsMode());
        loadingHolder.remove();
        hud.setUndoEnabled(false);
        hud.setHintEnabled(!challengeRules.noHints());
        buildBoard();

        if (!dailyChallenge && !game.save().tutorialSeen && levelIndex == 1) {
            stage.addAction(Actions.delay(0.7f, Actions.run(() -> {
                if (!disposed) {
                    showHowToPlay(stage, skin, () -> game.save().markTutorialSeen());
                }
            })));
        }
    }

    /**
     * Par arrives late, so the move limit does too. Until then the counter simply shows moves
     * played and nothing can be lost — better than guessing a limit and having to correct it
     * under the player's nose.
     */
    private void onParReady(int par) {
        if (disposed) {
            return;
        }
        if (level == null) {
            if (pendingLevel != null) {
                // Par beat the deal-in to the render thread; apply it right after the board.
                pendingPar = par;
                hasPendingPar = true;
            }
            return;
        }
        parMoves = par;
        level = level.withPar(par);
        moveLimit = challengeRun && challengeRules.parMoveLimit() ? par
            : dailyChallenge ? Difficulty.dailyMoveLimitFor(par)
            : Difficulty.moveLimitFor(par);
        hud.showPar(par);
        hud.setMoves(moves, moveLimit);
        if (phase == Phase.PLAYING && isOutOfMoves()) {
            outOfMoves();
        }
    }

    // ------------------------------------------------------------------- layout
    /** Rebuilds tubes and balls from scratch for the current {@link #state}. */
    private void buildBoard() {
        tubeGroup.clear();
        ballGroup.clear();
        tubes.clear();
        stacks.clear();
        hintGhost = null;
        suggestedFrom = -1;
        suggestedTo = -1;

        layoutTubes();

        for (int index = 0; index < state.tubeCount(); index++) {
            Array<BallActor> stack = new Array<>(Packed.CAPACITY);
            stacks.add(stack);

            int encoded = state.encodedAt(index);
            TubeActor tube = tubes.get(index);
            for (int slot = 0; slot < Packed.size(encoded); slot++) {
                BallActor ball = new BallActor(skin, Packed.slot(encoded, slot), ballSize,
                    game.save().colorLabels);
                ball.setPosition(tube.ballX(), tube.ballY(slot));
                ballGroup.addActor(ball);
                stack.add(ball);

                // Deal the board in: balls drop into place one after another.
                ball.getColor().a = 0f;
                ball.addAction(Actions.delay(0.02f * (index * Packed.CAPACITY + slot),
                    Actions.parallel(
                        Actions.fadeIn(0.18f),
                        Actions.sequence(
                            Actions.moveBy(0f, 40f, 0f),
                            Actions.moveBy(0f, -40f, 0.26f, Interpolation.swingOut)))));
            }
        }
        game.sound().play(SoundManager.Cue.START);
    }

    /** World units the HUD keeps above the board in the current orientation. */
    private float hudTopReserve() {
        return isLandscape() ? HudTable.LANDSCAPE_TOP : HudTable.PORTRAIT_TOP;
    }

    /**
     * Positions one tube actor per model tube, centred in the area the HUD leaves free: between
     * the top bar and the verb row in portrait, between the two side columns in landscape.
     * {@link BoardLayout} picks the row count that gives the largest balls.
     */
    private void layoutTubes() {
        int count = state.tubeCount();
        float worldWidth = stage.getWidth();
        float worldHeight = stage.getHeight();

        float areaLeft;
        float areaRight;
        float areaBottom;
        // The fixed reserves are a floor; the measured HUD edges win when the chrome is taller
        // (display cutout pushing the safe area down, longer status text, larger font).
        float areaTop = Math.min(worldHeight - hudTopReserve(), hud.chromeTopEdge() - HUD_BOARD_GAP);
        int maxPerRow;
        if (isLandscape()) {
            areaLeft = HudTable.LANDSCAPE_SIDE;
            areaRight = worldWidth - HudTable.LANDSCAPE_SIDE;
            areaBottom = Math.max(40f, hud.chromeBottomEdge() + 20f);
            maxPerRow = 12;
        } else {
            float available = Math.min(worldWidth, GameConfig.WORLD_WIDTH + 120f) - 2f * BOARD_MARGIN_X;
            areaLeft = (worldWidth - available) * 0.5f;
            areaRight = areaLeft + available;
            areaBottom = Math.max(HudTable.PORTRAIT_BOTTOM, hud.chromeBottomEdge() + HUD_BOARD_GAP);
            maxPerRow = MAX_TUBES_PER_ROW;
        }
        BoardLayout fit = BoardLayout.fit(count, areaRight - areaLeft, areaTop - areaBottom,
            MAX_TUBE_WIDTH, TUBE_GAP, ROW_GAP, maxPerRow, TubeActor.INNER_PADDING, Packed.CAPACITY);
        int rows = fit.rows;
        int perRow = fit.perRow;
        float tubeWidth = fit.tubeWidth;
        ballSize = fit.ballSize;
        float tubeHeight = fit.tubeHeight;

        float stackHeight = fit.stackHeight(ROW_GAP);
        float boardCenterY = (areaBottom + areaTop) * 0.5f;
        float boardCenterX = (areaLeft + areaRight) * 0.5f;
        float bottomY = boardCenterY - stackHeight * 0.5f;

        int placed = 0;
        for (int row = 0; row < rows; row++) {
            int inRow = Math.min(perRow, count - placed);
            float rowWidth = inRow * tubeWidth + TUBE_GAP * (inRow - 1);
            float rowX = boardCenterX - rowWidth * 0.5f;
            // Row 0 is the top row, so it gets the higher y.
            float rowY = bottomY + (rows - 1 - row) * (tubeHeight + ROW_GAP);

            for (int column = 0; column < inRow; column++) {
                final int index = placed + column;
                TubeActor tube = new TubeActor(skin, game.tubes(), index, ballSize);
                tube.setSize(tubeWidth, tubeHeight);
                tube.setHome(rowX + column * (tubeWidth + TUBE_GAP), rowY);
                tubeGroup.addActor(tube);
                tubes.add(tube);
            }
            placed += inRow;
        }
    }

    /** Re-lays the tubes (after a booster) and slides every ball to its new slot. */
    private void relayout() {
        clearSuggestion();
        tubeGroup.clear();
        tubes.clear();
        layoutTubes();

        for (int index = 0; index < stacks.size; index++) {
            TubeActor tube = tubes.get(index);
            Array<BallActor> stack = stacks.get(index);
            for (int slot = 0; slot < stack.size; slot++) {
                BallActor ball = stack.get(slot);
                ball.clearActions();
                ball.getColor().a = 1f; // a cut-short deal-in or fade must never leave a ball hidden
                ball.setScale(1f);
                ball.setSize(ballSize, ballSize);
                ball.setOrigin(Align.center); // origin is derived from size, so it must follow
                ball.addAction(Actions.moveTo(tube.ballX(), tube.ballY(slot), 0.22f,
                    Interpolation.swingOut));
            }
        }
    }

    // -------------------------------------------------------------------- input
    private void installTubeInput() {
        stage.getRoot().addCaptureListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y,
                                     int pointer, int button) {
                Actor target = event.getTarget();
                TubeActor tube = target.firstAscendant(TubeActor.class);
                if (phase != Phase.PLAYING || tube == null) return false;
                dragTube = tube.index();
                dragPointer = pointer;
                dragMoved = false;
                dragDownX = event.getStageX();
                dragDownY = event.getStageY();
                dragStartX = dragDownX;
                dragStartY = dragDownY;
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (pointer != dragPointer || dragTube < 0 || phase != Phase.PLAYING) return;
                float deltaX = event.getStageX() - dragDownX;
                float deltaY = event.getStageY() - dragDownY;
                if (!dragMoved && deltaX * deltaX + deltaY * deltaY
                    >= DRAG_THRESHOLD * DRAG_THRESHOLD
                    && state.encodedAt(dragTube) != Packed.EMPTY) {
                    if (selected != dragTube) {
                        deselect();
                        select(dragTube);
                    }
                    dragMoved = true;
                }
                if (dragMoved) {
                    positionDraggedRun(dragTube, event.getStageX() - dragStartX,
                        event.getStageY() - dragStartY);
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (pointer != dragPointer || dragTube < 0) return;
                int source = dragTube;
                if (dragMoved && phase == Phase.PLAYING) {
                    positionDraggedRun(source, event.getStageX() - dragStartX,
                        event.getStageY() - dragStartY);
                    // Aim with the balls the player sees, not the finger: the run is drawn
                    // lifted above the touch, so on a two-row board the finger often sits in
                    // the gap between rows, or on a tube of the other row.
                    int destination = dropTargetFor(source, event.getStageX() - dragStartX,
                        event.getStageY() - dragStartY);
                    if (destination >= 0
                        && MoveValidator.moveFor(state, source, destination) != null) {
                        onTubeTapped(destination);
                    } else {
                        positionDraggedRun(source, 0f, 0f);
                        if (destination >= 0) {
                            onTubeTapped(destination);
                        } else {
                            deselect();
                        }
                    }
                } else {
                    onTubeTapped(source);
                }
                dragTube = -1;
                dragPointer = -1;
                dragMoved = false;
            }
        });
    }

    private void positionDraggedRun(int index, float deltaX, float deltaY) {
        if (selected != index) return;
        int encoded = state.encodedAt(index);
        int size = Packed.size(encoded);
        int run = Packed.topRun(encoded);
        TubeActor tube = tubes.get(index);
        Array<BallActor> stack = stacks.get(index);
        for (int slot = size - run; slot < size; slot++) {
            BallActor ball = stack.get(slot);
            ball.clearActions();
            ball.setScale(1f);
            ball.setPosition(tube.ballX() + deltaX,
                tube.ballY(slot) + GameConfig.BALL_LIFT_HEIGHT + deltaY);
            ball.toFront();
        }
    }

    /**
     * @return the tube under the centre of the dragged run (drawn by
     *     {@link #positionDraggedRun}), or -1 when it is over no tube
     */
    private int dropTargetFor(int source, float deltaX, float deltaY) {
        int encoded = state.encodedAt(source);
        int size = Packed.size(encoded);
        int run = Packed.topRun(encoded);
        TubeActor from = tubes.get(source);
        float lowest = from.ballY(size - run);
        float highest = from.ballY(size - 1);
        float centreX = from.ballX() + deltaX + ballSize * 0.5f;
        float centreY = (lowest + highest) * 0.5f + GameConfig.BALL_LIFT_HEIGHT + deltaY
            + ballSize * 0.5f;

        int count = tubes.size;
        float[] x = new float[count];
        float[] y = new float[count];
        float[] width = new float[count];
        float[] height = new float[count];
        for (int i = 0; i < count; i++) {
            TubeActor tube = tubes.get(i);
            x[i] = tube.getX();
            y[i] = tube.getY();
            width[i] = tube.getWidth();
            height[i] = tube.getHeight();
        }
        return DropTarget.pick(centreX, centreY, x, y, width, height, TUBE_GAP * 0.5f,
            GameConfig.BALL_LIFT_HEIGHT + ballSize * 0.5f);
    }

    private void onTubeTapped(int index) {
        if (phase != Phase.PLAYING) {
            return;
        }
        if (selected < 0) {
            if (state.encodedAt(index) != Packed.EMPTY) {
                select(index);
                game.sound().play(SoundManager.Cue.PICK);
            }
            return;
        }
        if (selected == index) {
            deselect();
            return;
        }

        Move move = MoveValidator.moveFor(state, selected, index);
        if (move == null) {
            // The tap was not a legal drop, but the tube almost certainly makes a fine new
            // source — so say no clearly, then pick it up rather than making the player tap
            // twice.
            tubes.get(index).shake();
            tubes.get(index).flash(Palette.DANGER, 0.3f);
            game.sound().play(SoundManager.Cue.INVALID);
            haptic(18);
            deselect();
            if (state.encodedAt(index) != Packed.EMPTY) {
                select(index);
            }
            return;
        }
        applyMove(move, true);
    }

    private void select(int index) {
        selected = index;
        tubes.get(index).setHighlighted(true);
        liftTopRun(index, GameConfig.BALL_LIFT_HEIGHT);
    }

    private void deselect() {
        if (selected < 0) {
            return;
        }
        int previous = selected;
        selected = -1;
        tubes.get(previous).setHighlighted(false);
        liftTopRun(previous, 0f);
    }

    /** Raises (or lowers) the balls the player has picked up. */
    private void liftTopRun(int index, float lift) {
        int encoded = state.encodedAt(index);
        int run = Packed.topRun(encoded);
        int size = Packed.size(encoded);
        Array<BallActor> stack = stacks.get(index);
        TubeActor tube = tubes.get(index);

        for (int slot = size - run; slot < size && slot < stack.size; slot++) {
            if (slot < 0) {
                continue;
            }
            BallActor ball = stack.get(slot);
            ball.clearActions();
            ball.toFront();
            ball.setScale(1f);
            Action rise = Actions.moveTo(tube.ballX(), tube.ballY(slot) + lift, 0.12f,
                Interpolation.swingOut);
            if (lift > 0f) {
                // Held balls grow a touch and bob gently, slightly out of step, so the
                // selection reads as "picked up" rather than merely moved.
                float bob = GameConfig.BALL_LIFT_HEIGHT * 0.12f;
                ball.addAction(Actions.sequence(
                    Actions.parallel(rise,
                        Actions.scaleTo(1.07f, 1.07f, 0.12f, Interpolation.swingOut)),
                    Actions.delay(0.04f * (size - 1 - slot)),
                    Actions.forever(Actions.sequence(
                        Actions.moveBy(0f, bob, 0.42f, Interpolation.sine),
                        Actions.moveBy(0f, -bob, 0.42f, Interpolation.sine)))));
            } else {
                ball.addAction(Actions.parallel(rise,
                    Actions.scaleTo(1f, 1f, 0.12f, Interpolation.sineOut)));
            }
        }
    }

    // --------------------------------------------------------------------- play
    private void applyMove(Move move, boolean record) {
        if (record) {
            stateHistory.add(state);
            moveHistory.add(move);
        }
        deselect();
        clearSuggestion();
        transferBalls(move.from(), move.to(), move.count());
        state = state.apply(move);

        if (record) {
            moves++;
            haptic(12);
        }
        hud.setMoves(moves, moveLimit);
        hud.setUndoEnabled(!challengeRules.noUndo() && stateHistory.size > 0);
        // The drop sound plays per ball as it lands - see transferBalls.

        boolean won = WinChecker.isWon(state);
        if (record && !won && Packed.isComplete(state.encodedAt(move.to()))) {
            final int completed = move.to();
            float landed = GameConfig.BALL_MOVE_DURATION
                + (move.count() - 1) * GameConfig.BALL_MOVE_STAGGER + 0.12f; // after the squash
            stage.addAction(Actions.delay(landed, Actions.run(() -> celebrateTube(completed))));
        }

        if (won) {
            haptic(35);
            onWin();
            return;
        }
        if (isOutOfMoves()) {
            outOfMoves();
            return;
        }
        if (hintsUnlocked) {
            requestHint(false); // keep the remaining-moves indicator honest
        }
    }

    private void haptic(int milliseconds) {
        if (game.save().vibrate) {
            Gdx.input.vibrate(milliseconds);
        }
    }

    /** Animates {@code count} balls from the top of one tube onto another. */
    private void transferBalls(int from, int to, int count) {
        Array<BallActor> source = stacks.get(from);
        Array<BallActor> target = stacks.get(to);
        TubeActor sourceTube = tubes.get(from);
        TubeActor targetTube = tubes.get(to);
        float column = ballSize * 0.5f;

        for (int i = 0; i < count; i++) {
            BallActor ball = source.pop();
            int slot = target.size;
            target.add(ball);

            float destinationY = targetTube.ballY(slot);
            // Only clear the glass the ball is actually behind. A ball still in its source
            // column must climb out of it; a ball the player has already dragged away only
            // needs to get over the target's mouth - it must not climb back over the other
            // row first. A ball dragged straight above its slot just drops in.
            boolean inSource = Math.abs(ball.getX() - sourceTube.ballX()) < column
                && ball.getY() < sourceTube.liftY();
            boolean aboveSlot = Math.abs(ball.getX() - targetTube.ballX()) < column
                && ball.getY() >= destinationY;
            float arc;
            if (inSource) {
                float clearance = Math.max(sourceTube.liftY(), targetTube.liftY());
                arc = Math.max(0f, clearance - Math.max(ball.getY(), destinationY)) + 18f;
            } else if (aboveSlot) {
                arc = 0f;
            } else {
                arc = Math.max(0f, targetTube.liftY() - Math.max(ball.getY(), destinationY)) + 6f;
            }

            ball.clearActions();
            ball.toFront();
            ball.setScale(1f);
            // Each landing clinks a step higher than the last, so a run of balls plays a scale.
            final float pitch = 0.9f + 0.08f * slot;
            ball.addAction(Actions.sequence(
                Actions.delay(i * GameConfig.BALL_MOVE_STAGGER),
                ArcMoveAction.arcTo(targetTube.ballX(), destinationY, arc,
                    GameConfig.BALL_MOVE_DURATION),
                Actions.run(() -> {
                    game.sound().play(SoundManager.Cue.DROP, pitch);
                    // Contact: a soft ring in the ball's colour spreads from the landing spot.
                    Ripple.at(skin, ballGroup, ball.getX() + ball.getWidth() * 0.5f,
                        ball.getY() + ball.getHeight() * 0.12f, ball.getWidth(), ball.getColor());
                    ball.toFront();
                }),
                Actions.scaleTo(1.16f, 0.84f, 0.05f, Interpolation.sineOut),
                Actions.scaleTo(1f, 1f, 0.18f, Interpolation.swingOut)));
        }
    }

    // ------------------------------------------------------------------ juice
    private static final String[] PRAISE = {T.k("Nice!"), T.k("Great!"), T.k("Awesome!"), T.k("Amazing!")};
    private static final SoundManager.Cue[] PRAISE_VOICE = {
        SoundManager.Cue.VOICE_NICE, SoundManager.Cue.VOICE_GREAT,
        SoundManager.Cue.VOICE_AWESOME, SoundManager.Cue.VOICE_PERFECT};
    private int tubesCompleted;

    /** A tube was just finished: its balls hop in a wave, sparks fly, and a word pops up. */
    private void celebrateTube(int index) {
        if (disposed || phase != Phase.PLAYING || index >= stacks.size || index == selected
            || !Packed.isComplete(state.encodedAt(index))) {
            return; // the player already moved on (or undid it) - don't celebrate stale news
        }
        TubeActor tube = tubes.get(index);
        Array<BallActor> stack = stacks.get(index);
        for (int slot = 0; slot < stack.size; slot++) {
            BallActor ball = stack.get(slot);
            ball.clearActions();
            ball.setScale(1f); // a landing squash may still be running
            ball.setPosition(tube.ballX(), tube.ballY(slot));
            ball.addAction(Actions.sequence(
                Actions.delay(slot * 0.05f),
                Actions.moveBy(0f, 26f, 0.14f, Interpolation.sineOut),
                Actions.moveBy(0f, -26f, 0.24f, Interpolation.bounceOut)));
        }
        float topY = tube.getY() + tube.getHeight();
        burst.sparkle(tube.getX() + tube.getWidth() * 0.5f, topY,
            Packed.top(state.encodedAt(index)), 26);
        game.sound().play(SoundManager.Cue.TUBE);
        tube.flash(Palette.GOLD, 0.7f); // the tube itself glows, not only its balls

        int step = Math.min(tubesCompleted, PRAISE.length - 1);
        tubesCompleted++;
        popWord(T.r(PRAISE[step]), tube.getX() + tube.getWidth() * 0.5f);
        game.sound().play(PRAISE_VOICE[step]);
    }

    private Actor currentWord;

    /**
     * A word that springs up above the board, floats and fades - used for praise. Only one is
     * shown at a time: a new word sends the previous one away, so they never stack up.
     */
    private void popWord(String text, float centerX) {
        if (currentWord != null) {
            currentWord.clearActions();
            currentWord.addAction(Actions.sequence(Actions.fadeOut(0.1f), Actions.removeActor()));
        }
        float boardTop = 0f;
        for (TubeActor tube : tubes) {
            boardTop = Math.max(boardTop, tube.getY() + tube.getHeight());
        }
        Label label = new Label(text, skin, "display");
        label.setColor(Palette.GOLD);
        Container<Label> box = new Container<>(label);
        box.setTransform(true);
        box.pack();
        box.setOrigin(Align.center);
        float halfWidth = box.getWidth() * 0.5f;
        float x = Math.max(halfWidth + 16f, Math.min(stage.getWidth() - halfWidth - 16f, centerX));
        // In the gap between the board and the HUD; it rises 60 px, so leave room under the HUD.
        float y = Math.min(boardTop + 12f,
            stage.getHeight() - hudTopReserve() + 10f - box.getHeight() - 60f);
        box.setPosition(x - halfWidth, y);
        box.setTouchable(Touchable.disabled);
        box.setScale(0.2f);
        box.getColor().a = 0f;
        stage.addActor(box);
        currentWord = box;
        burst.toFront(); // the burst has to stay the last actor (it interrupts the batch)
        box.addAction(Actions.sequence(
            Actions.parallel(
                Actions.fadeIn(0.12f),
                Actions.scaleTo(1.1f, 1.1f, 0.24f, Interpolation.swingOut),
                Actions.moveBy(0f, 36f, 0.8f, Interpolation.pow2Out)),
            Actions.scaleTo(1f, 1f, 0.12f),
            Actions.delay(0.2f),
            Actions.parallel(Actions.fadeOut(0.3f), Actions.moveBy(0f, 24f, 0.3f)),
            Actions.removeActor()));
    }

    @Override
    public void onUndo() {
        if (challengeRules.noUndo() || phase != Phase.PLAYING || stateHistory.size == 0) {
            return;
        }
        // Free undos first, then the stock; when both are gone, offer a video or a pack.
        int taken = Assists.take(freeUndos, game.save().stockOf(Assists.UNDO));
        if (taken == 0) {
            offerAssist(Assists.UNDO);
            return;
        }
        if (taken == 1) {
            freeUndos--;
        } else {
            game.save().consumeAssist(Assists.UNDO);
        }
        refreshStock();
        deselect();
        clearSuggestion();
        Move move = moveHistory.pop();
        BoardState previous = stateHistory.pop();

        transferBalls(move.to(), move.from(), move.count());
        state = previous;

        // Undo gives the move back. It has to: with a limit in play, an undo that still cost a
        // move would make the button a trap rather than a safety net.
        moves = Math.max(0, moves - 1);
        hud.setMoves(moves, moveLimit);
        hud.setUndoEnabled(!challengeRules.noUndo() && stateHistory.size > 0);
        game.sound().play(SoundManager.Cue.BUTTON);

        if (hintsUnlocked) {
            requestHint(false);
        }
    }

    @Override
    public void onRestart() {
        if (phase == Phase.WON || level == null) {
            return;
        }
        if (moves == 0 && stateHistory.size == 0) {
            return; // nothing to reset: don't charge for a no-op
        }
        // One free reset per level, then the stock; when both are gone, offer a video or a pack.
        int taken = Assists.take(freeResets, game.save().stockOf(Assists.RESET));
        if (taken == 0) {
            offerAssist(Assists.RESET);
            return;
        }
        if (taken == 1) {
            freeResets--;
        } else {
            game.save().consumeAssist(Assists.RESET);
        }
        deselect();
        clearSuggestion();
        stateHistory.clear();
        moveHistory.clear();
        state = level.start();
        moves = 0;
        freeBoosters = GameConfig.FREE_BOOSTERS_PER_LEVEL;
        freeUndos = Assists.UNDO.freePerLevel();
        freeHints = GameConfig.FREE_HINTS_PER_LEVEL;
        phase = Phase.PLAYING;

        hud.setMoves(0, moveLimit);
        hud.setUndoEnabled(false);
        refreshStock();
        hud.setHintCaption(hintCaption());
        hud.setHintEnabled(!challengeRules.noHints());
        hud.showPar(parMoves);
        buildBoard();
        game.sound().play(SoundManager.Cue.BUTTON);
    }

    @Override
    public void onBooster() {
        if (challengeRules.noBoosters() || phase != Phase.PLAYING) {
            return;
        }
        if (state.tubeCount() >= MAX_TUBES) {
            toast(T.r("No room for another tube"), Palette.DANGER);
            game.sound().play(SoundManager.Cue.INVALID);
            return;
        }
        SaveData save = game.save();
        // Free tubes first, then the stock; when both are gone, offer a video or a pack.
        int taken = Assists.take(freeBoosters, save.stockOf(Assists.TUBE));
        if (taken == 0) {
            offerAssist(Assists.TUBE);
            return;
        }
        if (taken == 1) {
            freeBoosters--;
        } else {
            save.consumeAssist(Assists.TUBE);
        }

        deselect();
        state = state.withExtraTube();
        stacks.add(new Array<>(Packed.CAPACITY));
        relayout();

        refreshStock();
        hud.setCoins(save.coins);
        game.sound().play(SoundManager.Cue.BUTTON);
        if (hintsUnlocked) {
            requestHint(false);
        }
    }

    @Override
    public void onBack() {
        game.goHome();
    }

    // --------------------------------------------------------------------- audio
    // Same switches as Settings (persisted in SaveData), reachable without leaving the level.
    @Override
    public void onToggleEffects() {
        boolean on = !game.sound().areEffectsOn();
        game.sound().setEffectsEnabled(on);
        hud.setAudio(on, game.sound().isMusicOn());
        game.sound().play(SoundManager.Cue.BUTTON); // audible confirmation only when turned on
    }

    @Override
    public void onToggleMusic() {
        boolean on = !game.sound().isMusicOn();
        game.sound().setMusicEnabled(on);
        hud.setAudio(game.sound().areEffectsOn(), on);
        game.sound().play(SoundManager.Cue.BUTTON);
    }

    private int availableBoosters() {
        return challengeRules.noBoosters() ? 0 : freeBoosters + game.save().boosters;
    }

    /** The stock strip: free uses left this level plus what the save holds. */
    private void refreshStock() {
        SaveData save = game.save();
        hud.setStock(availableBoosters(),
            challengeRules.noUndo() ? 0 : freeUndos + save.stockOf(Assists.UNDO),
            freeResets + save.stockOf(Assists.RESET));
    }

    /**
     * Out of an assist: one card, two honest ways to get more - a video when a network has one
     * ready (and, for tubes, the cooldown has passed), or coins - and a way to say no.
     */
    private void offerAssist(final Assists assist) {
        SaveData save = game.save();
        boolean videoReady = game.ads().isRewardedReady()
            && (assist != Assists.TUBE || save.rewardedTubeCooldownRemaining(System.currentTimeMillis()) == 0L);
        boolean canBuy = save.coins >= assist.packPrice();
        game.sound().play(SoundManager.Cue.INVALID);
        Dialog dialog = Ui.dialog(skin, assist == com.ben.laama.sortpuzzle.logic.Assists.TUBE ? T.r("Out of extra tubes")
            : assist == com.ben.laama.sortpuzzle.logic.Assists.UNDO ? T.r("Out of undos") : T.r("Out of resets"), choice -> {
            if ("video".equals(choice)) {
                watchForAssist(assist);
            } else if ("buy".equals(choice) && !disposed && save.buyAssistPack(assist)) {
                hud.setCoins(save.coins);
                game.sound().play(SoundManager.Cue.COIN);
                refreshStock();
                toast(T.f("+{0} in stock", assist.packLabel()), Palette.SUCCESS);
            }
        });
        Table content = dialog.getContentTable();
        String ways = videoReady && canBuy
            ? T.f("Watch a short video for {0}, or pay {1} coins for {2}.", assist.videoLabel(),
                assist.packPrice(), assist.packLabel())
            : videoReady ? T.f("Watch a short video for {0}.", assist.videoLabel())
            : canBuy ? T.f("{0} for {1} coins.", assist.packLabel(), assist.packPrice())
            : T.f("{0} cost {1} coins. Clear levels to earn more, or come back when a video is ready.",
                assist.packLabel(), assist.packPrice());
        content.add(Ui.paragraph(skin, ways, "dim", 420f)).width(420f).padTop(6f).row();
        dialog.button(Ui.button(skin, T.r("NOT NOW"), "ghost", () -> { }), "no");
        if (videoReady) {
            dialog.button(Ui.iconTextButton(skin, "icon-play", T.r("WATCH"), "primary", () -> { }), "video");
        }
        if (canBuy) {
            dialog.button(Ui.iconTextButton(skin, "icon-coin", T.f("BUY  {0}", assist.packPrice()), "gold",
                () -> { }), "buy");
        }
        dialog.show(stage);
    }

    private void watchForAssist(final Assists assist) {
        game.pacing().onFullScreenAdShown();
        game.ads().showRewarded(new AdProvider.RewardListener() {
            @Override
            public void onRewarded(int coins) {
                if (disposed) return;
                SaveData save = game.save();
                if (assist == Assists.TUBE) {
                    save.claimRewardedTube(System.currentTimeMillis());
                } else {
                    save.addAssist(assist, assist.videoReward());
                }
                game.analytics().event(AnalyticsEvents.REWARDED_EXTRA_MOVES, AnalyticsEvents.P_LEVEL,
                    levelIndex, AnalyticsEvents.P_SOURCE, "rewarded", AnalyticsEvents.P_MODE,
                    "assist." + assist.id());
                refreshStock();
                game.sound().play(SoundManager.Cue.TUBE);
                toast(T.f("+{0} in stock", assist.videoLabel()), Palette.SUCCESS);
            }

            @Override
            public void onFailed() {
                if (!disposed) {
                    toast(T.r("Video closed early — nothing added this time"), Palette.TEXT_DIM);
                }
            }
        });
    }

    // --------------------------------------------------------------------- hints
    @Override
    public void onHint() {
        if (challengeRules.noHints() || phase != Phase.PLAYING || hintPending) {
            return;
        }
        if (hintsUnlocked) {
            requestHint(true);
            return;
        }
        if (freeHints > 0) {
            freeHints--;
            hud.setHintCaption(hintCaption());
            requestHint(true);
            return;
        }
        // Out of free hints. One unlock buys hints for the rest of the level, not a single
        // move — a player stuck enough to pay is not helped by being asked again four moves
        // later. Paid with a video when a network has one ready, with coins otherwise.
        RewardOffer offer = RewardOffer.decide(game.ads().isRewardedReady(),
            game.save().coins, GameConfig.HINT_UNLOCK_PRICE);
        switch (offer.kind()) {
            case VIDEO:
                hud.setHintEnabled(false);
                game.pacing().onFullScreenAdShown();
                game.ads().showRewarded(new AdProvider.RewardListener() {
                    @Override
                    public void onRewarded(int coins) {
                        if (!disposed) {
                            unlockHints();
                        }
                    }

                    @Override
                    public void onFailed() {
                        if (!disposed) {
                            hud.setHintEnabled(!challengeRules.noHints());
                            toast(T.r("Video closed early — no hint this time"), Palette.TEXT_DIM);
                        }
                    }
                });
                break;
            case COINS:
                confirmHintUnlock(offer.price());
                break;
            case NONE:
            default:
                game.sound().play(SoundManager.Cue.INVALID);
                toast(T.f("Hints cost {0} coins — clear a level to earn more", offer.price()),
                    Palette.TEXT_DIM);
                break;
        }
    }

    private void confirmHintUnlock(int price) {
        Dialog dialog = Ui.dialog(skin, T.r("Unlock hints?"), choice -> {
            if (Boolean.TRUE.equals(choice) && !disposed && game.save().spendCoins(price)) {
                hud.setCoins(game.save().coins);
                game.sound().play(SoundManager.Cue.COIN);
                unlockHints();
            }
        });
        Table content = dialog.getContentTable();
        content.add(Ui.paragraph(skin, T.f("Hints for the rest of this level, for {0} coins.", price),
            "dim", 400f)).width(400f).padTop(6f).row();
        dialog.button(Ui.button(skin, T.r("NOT NOW"), "ghost", () -> { }), Boolean.FALSE);
        dialog.button(Ui.button(skin, T.f("UNLOCK  •  {0}", price), "gold", () -> { }), Boolean.TRUE);
        dialog.show(stage);
    }

    private void unlockHints() {
        game.analytics().event(AnalyticsEvents.HINTS_UNLOCKED,
            AnalyticsEvents.P_LEVEL, levelIndex, AnalyticsEvents.P_MODE, analyticsMode());
        hintsUnlocked = true;
        hud.setHintCaption(hintCaption());
        hud.setHintEnabled(!challengeRules.noHints());
        requestHint(true);
    }

    /**
     * Asks the solver what to do, on a worker thread.
     *
     * @param animate {@code true} to play the suggestion on the board, {@code false} to refresh
     *                the remaining-moves indicator quietly after a move
     */
    private void requestHint(boolean animate) {
        if (hintPending || state == null) {
            return;
        }
        hintPending = true;
        hud.setHintEnabled(false);

        final int requestId = ++hintRequestId;
        final BoardState snapshot = state;
        Thread worker = new Thread(() -> {
            Hint hint = HintEngine.advise(snapshot);
            Gdx.app.postRunnable(() -> onHintReady(requestId, hint, animate));
        }, "hint-" + levelIndex);
        worker.setDaemon(true);
        worker.start();
    }

    private void onHintReady(int requestId, Hint hint, boolean animate) {
        if (disposed) {
            return;
        }
        hintPending = false;
        hud.setHintEnabled(phase == Phase.PLAYING);
        if (requestId != hintRequestId || phase != Phase.PLAYING) {
            return; // the board moved on while we were thinking
        }

        switch (hint.kind()) {
            case MOVE:
                hud.showRemaining(hint.remainingMoves(), hint.isExact());
                if (animate) {
                    showSuggestion(hint.move());
                    game.sound().play(SoundManager.Cue.PICK);
                }
                break;
            case UNDO:
                hud.showStatus(T.r("No way to win from here — undo"), Palette.DANGER);
                if (animate) {
                    game.sound().play(SoundManager.Cue.INVALID);
                }
                break;
            case UNKNOWN:
                hud.showStatus(T.r("Too tangled to read from here"), Palette.TEXT_DIM);
                break;
            case ALREADY_WON:
            default:
                break;
        }
    }

    /** Pulses the two tubes involved and flies a translucent ball along the suggested path. */
    private void showSuggestion(Move move) {
        clearSuggestion();

        suggestedFrom = move.from();
        suggestedTo = move.to();
        TubeActor source = tubes.get(suggestedFrom);
        TubeActor target = tubes.get(suggestedTo);
        source.setSuggested(true);
        target.setSuggested(true);
        game.sound().play(SoundManager.Cue.HINT);

        int topSlot = stacks.get(suggestedFrom).size - 1;
        if (topSlot < 0) {
            return;
        }
        float startX = source.ballX();
        float startY = source.ballY(topSlot);
        float endX = target.ballX();
        float endY = target.ballY(stacks.get(suggestedTo).size);
        float arc = Math.max(0f,
            Math.max(source.liftY(), target.liftY()) - Math.max(startY, endY)) + 18f;

        BallActor ghost = new BallActor(skin, move.colorIndex(), ballSize, game.save().colorLabels);
        ghost.getColor().a = 0f;
        ghost.setPosition(startX, startY);
        ballGroup.addActor(ghost);
        ghost.toFront();
        hintGhost = ghost;

        // Each repeat restarts the sequence, and ArcMoveAction reads its start position when it
        // begins — so the instant moveTo at the head is what stops the second pass from arcing
        // from the destination to itself.
        Action cycle = Actions.sequence(
            Actions.moveTo(startX, startY),
            Actions.alpha(0.7f, 0.10f),
            ArcMoveAction.arcTo(endX, endY, arc, 0.50f),
            Actions.alpha(0f, 0.14f));

        ghost.addAction(Actions.sequence(
            Actions.repeat(GameConfig.HINT_REPLAYS, cycle),
            Actions.run(() -> {
                if (hintGhost == ghost) {
                    clearSuggestion();
                }
            }),
            Actions.removeActor()));
    }

    private void clearSuggestion() {
        if (suggestedFrom >= 0 && suggestedFrom < tubes.size) {
            tubes.get(suggestedFrom).setSuggested(false);
        }
        if (suggestedTo >= 0 && suggestedTo < tubes.size) {
            tubes.get(suggestedTo).setSuggested(false);
        }
        suggestedFrom = -1;
        suggestedTo = -1;
        if (hintGhost != null) {
            hintGhost.clearActions();
            hintGhost.remove();
            hintGhost = null;
        }
    }

    private String hintCaption() {
        if (challengeRules.noHints()) {
            return T.r("Hint off");
        }
        if (hintsUnlocked) {
            return T.r("Hint ∞");
        }
        if (freeHints > 0) {
            return T.f("Hint ×{0}", freeHints);
        }
        return game.ads().isRewardedReady() ? T.r("Hint ▶") : T.f("Hint • {0}", GameConfig.HINT_UNLOCK_PRICE);
    }

    // -------------------------------------------------------------- out of moves
    private boolean isOutOfMoves() {
        return moveLimit > 0 && moves >= moveLimit;
    }

    private String analyticsMode() {
        return dailyChallenge ? "daily" : challengeRun ? "challenge" : "campaign";
    }

    private void outOfMoves() {
        phase = Phase.OUT_OF_MOVES;
        game.analytics().event(AnalyticsEvents.LEVEL_OUT_OF_MOVES,
            AnalyticsEvents.P_LEVEL, levelIndex, AnalyticsEvents.P_MOVES, moves,
            AnalyticsEvents.P_MODE, analyticsMode());
        deselect();
        clearSuggestion();
        hud.setHintEnabled(false);
        game.sound().play(SoundManager.Cue.INVALID);
        showOutOfMovesDialog();
    }

    private void showOutOfMovesDialog() {
        if (challengeRun) {
            Dialog ended = Ui.dialog(skin, T.r("Challenge ended"), choice -> {
                if ("retry".equals(choice)) {
                    onRestart();
                } else {
                    game.goHome();
                }
            });
            ended.getContentTable().add(new Label(T.r("This ruleset gives you no extra moves."),
                skin, "dim")).padTop(8f).row();
            ended.button(Ui.button(skin, T.r("HOME"), "ghost", () -> { }), "home");
            ended.button(Ui.button(skin, T.r("RETRY"), "primary", () -> { }), "retry");
            ended.show(stage);
            return;
        }
        RewardOffer offer = RewardOffer.decide(game.ads().isRewardedReady(),
            game.save().coins, GameConfig.EXTRA_MOVES_PRICE);

        Dialog dialog = Ui.dialog(skin, T.r("Out of moves"), choice -> {
            if ("video".equals(choice)) {
                watchAdForMoves();
            } else if ("coins".equals(choice)) {
                if (game.save().spendCoins(offer.price())) {
                    hud.setCoins(game.save().coins);
                    grantExtraMoves();
                } else {
                    showOutOfMovesDialog();
                }
            } else if ("restart".equals(choice)) {
                onRestart();
            } else {
                game.goHome();
            }
        });
        Table content = dialog.getContentTable();
        content.add(new Label(T.f("You used all {0} of them.", moveLimit), skin, "dim")).padTop(4f).row();
        if (!offer.isAvailable()) {
            content.add(Ui.paragraph(skin, T.f("+{0} moves cost {1} coins. Clear a level to earn more.",
                GameConfig.EXTRA_MOVES_PER_AD, offer.price()), "caption", 400f))
                .width(400f).padTop(10f).row();
        }

        dialog.button(Ui.button(skin, T.r("HOME"), "ghost", () -> { }), "home");
        dialog.button(Ui.button(skin, T.r("RETRY"), "secondary", () -> { }), "restart");
        if (offer.kind() == RewardOffer.Kind.VIDEO) {
            dialog.button(Ui.button(skin, "+" + GameConfig.EXTRA_MOVES_PER_AD + "  ▶", "gold",
                () -> { }), "video");
        } else if (offer.kind() == RewardOffer.Kind.COINS) {
            dialog.button(Ui.button(skin, "+" + GameConfig.EXTRA_MOVES_PER_AD + "  •  "
                + offer.price(), "gold", () -> { }), "coins");
        }
        dialog.show(stage);
    }

    private void watchAdForMoves() {
        game.pacing().onFullScreenAdShown();
        game.ads().showRewarded(new AdProvider.RewardListener() {
            @Override
            public void onRewarded(int coins) {
                if (!disposed) {
                    grantExtraMoves();
                }
            }

            @Override
            public void onFailed() {
                if (!disposed) {
                    toast(T.r("Video closed early — no extra moves"), Palette.TEXT_DIM);
                    showOutOfMovesDialog();
                }
            }
        });
    }

    private void grantExtraMoves() {
        moveLimit += GameConfig.EXTRA_MOVES_PER_AD;
        game.analytics().event(AnalyticsEvents.REWARDED_EXTRA_MOVES,
            AnalyticsEvents.P_LEVEL, levelIndex, AnalyticsEvents.P_MODE, analyticsMode());
        phase = Phase.PLAYING;
        hud.setMoves(moves, moveLimit);
        hud.setHintEnabled(!challengeRules.noHints());
        toast(T.f("+{0} moves", GameConfig.EXTRA_MOVES_PER_AD), Palette.SUCCESS);
        game.sound().play(SoundManager.Cue.COIN);
    }

    private void toast(String message, com.badlogic.gdx.graphics.Color color) {
        Ui.toast(stage, skin, message, color);
    }

    // ----------------------------------------------------------------------- win
    private void onWin() {
        phase = Phase.WON;
        deselect();
        clearSuggestion();
        hud.setHintEnabled(false);
        stage.addAction(Actions.sequence(Actions.delay(0.5f), Actions.run(this::celebrate)));
    }

    private void celebrate() {
        if (disposed) {
            return;
        }
        burst.fire(stage.getWidth() * 0.5f, stage.getHeight() * 0.5f, state.colorCount());
        game.sound().play(SoundManager.Cue.WIN);

        // Every ball jumps, in a wave from left to right, before the result panel arrives.
        for (int index = 0; index < stacks.size; index++) {
            Array<BallActor> stack = stacks.get(index);
            for (int slot = 0; slot < stack.size; slot++) {
                BallActor ball = stack.get(slot);
                ball.clearActions();
                ball.setScale(1f);
                ball.setPosition(tubes.get(index).ballX(), tubes.get(index).ballY(slot));
                ball.addAction(Actions.sequence(
                    Actions.delay(index * 0.05f + slot * 0.03f),
                    Actions.moveBy(0f, 34f, 0.16f, Interpolation.sineOut),
                    Actions.moveBy(0f, -34f, 0.3f, Interpolation.bounceOut)));
            }
        }
        boolean perfect = parMoves != Level.PAR_UNKNOWN && moves <= parMoves;
        popWord(perfect ? T.r("Perfect!") : T.r("Complete!"), stage.getWidth() * 0.5f);
        SoundManager.Cue voice = perfect ? SoundManager.Cue.VOICE_PERFECT
            : SoundManager.Cue.VOICE_LEVEL_COMPLETE;
        game.sound().play(voice);

        boolean paletteWasUnlocked = game.save().dailyPaletteUnlocked;
        // Summarise the run before the save moves on, so "new record" compares with the old best.
        int previousBest = dailyChallenge || challengeRun ? 0 : game.save().bestMovesFor(levelIndex);
        final WinSummary summary = WinSummary.of(moves, parMoves, previousBest);
        int earned;
        if (dailyChallenge) {
            earned = game.save().recordDailyWin(dailyDate);
        } else if (challengeRun) {
            earned = game.save().recordChallengeWin();
        } else {
            // Star-road and chapter skins this win unlocks: owned after, not before.
            java.util.List<Cosmetic> ownedBefore = Shop.ownedProgressionItems(game.save());
            earned = game.save().recordWin(levelIndex, moves, parMoves);
            java.util.List<Cosmetic> unlockedNow = Shop.ownedProgressionItems(game.save());
            unlockedNow.removeAll(ownedBefore);
            starUnlocks = unlockedNow;
        }
        // Leaderboards and analytics: fire-and-forget, inert without the platform services.
        GameServices services = game.services();
        services.submitScore(GameServices.Leaderboard.HIGHEST_LEVEL,
            LeaderboardScores.highestLevel(game.save().highestUnlocked));
        services.submitScore(GameServices.Leaderboard.TOTAL_STARS,
            LeaderboardScores.totalStars(game.save().totalStars()));
        if (dailyChallenge) {
            services.submitScore(GameServices.Leaderboard.DAILY_FEWEST_MOVES,
                LeaderboardScores.dailyMoves(moves));
        }
        game.analytics().event(dailyChallenge ? AnalyticsEvents.DAILY_COMPLETE : AnalyticsEvents.LEVEL_COMPLETE,
            AnalyticsEvents.P_LEVEL, levelIndex, AnalyticsEvents.P_MOVES, moves,
            AnalyticsEvents.P_PAR, parMoves, AnalyticsEvents.P_STARS, summary.stars,
            AnalyticsEvents.P_PERFECT, summary.perfect, AnalyticsEvents.P_MODE, analyticsMode());
        boolean paletteUnlockedNow = !paletteWasUnlocked && game.save().dailyPaletteUnlocked;
        // The purse counts up as the coins land on it (see showResult), so the HUD keeps the
        // old total until then.
        final int coinsBefore = Math.max(0, game.save().coins - earned);
        hud.setCoins(coinsBefore);
        final int awardedCoins = earned;
        stage.addAction(Actions.delay(0.9f, Actions.run(() -> {
            if (!disposed) {
                showResult(summary, awardedCoins, coinsBefore, paletteUnlockedNow);
            }
        })));
    }

    /**
     * Renders the result card in the player's current look and hands it to the share sheet.
     * Off Android there is no sheet: the button says so instead of doing nothing.
     */
    private void shareResult(WinSummary summary) {
        if (!game.share().isAvailable()) {
            Ui.toast(stage, skin, T.r("Sharing works in the phone version"), Palette.TEXT_DIM);
            return;
        }
        String heading;
        if (dailyChallenge) {
            heading = T.f("Daily puzzle  •  {0}", LocalDate.parse(dailyDate).format(
                java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", new java.util.Locale(T.language().code))));
        } else if (challengeRun) {
            heading = T.f("Challenge  •  level {0}", LevelNumbers.format(levelIndex));
        } else {
            heading = T.f("Level {0}  •  {1} colours", LevelNumbers.format(levelIndex), level.colors());
        }
        int par = parMoves == Level.PAR_UNKNOWN ? 0 : parMoves;
        ShareCard.Result result = new ShareCard.Result(heading, level.colors(), level.emptyTubes(),
            summary.stars, moves, par, summary.perfect);
        com.badlogic.gdx.files.FileHandle png;
        try {
            png = ShareCard.render(skin, game.tubes(), game.batch(), stage.getViewport(), result);
        } catch (RuntimeException failed) {
            com.badlogic.gdx.Gdx.app.error("Share", "Result card failed: " + failed.getMessage());
            Ui.toast(stage, skin, T.r("Couldn't build the picture. Try again."), Palette.TEXT_DIM);
            return;
        }
        // Platform text: logical order, the share sheet lays it out itself.
        String what = dailyChallenge ? T.raw("today's Tidy Tubes puzzle")
            : T.rawf("level {0} of Tidy Tubes", LevelNumbers.format(levelIndex));
        String text = (moves == 1 ? T.rawf("I solved {0} in 1 move", what)
            : T.rawf("I solved {0} in {1} moves", what, moves))
            + (par > 0 ? T.rawf(" (best possible {0})", par) : "") + T.raw(". Can you beat it? ")
            + GameConfig.STORE_URL;
        game.analytics().event(AnalyticsEvents.SHARE_RESULT, AnalyticsEvents.P_LEVEL, levelIndex,
            AnalyticsEvents.P_MODE, analyticsMode());
        game.share().shareImage(png, text, T.raw("Share your result"));
    }

    /**
     * Thirty-six seconds of guided breathing over the board, then straight on to the next
     * level: the breath is the break, so no interstitial follows it.
     */
    private void startBreath() {
        game.analytics().event(AnalyticsEvents.BREATHE_START, AnalyticsEvents.P_LEVEL, levelIndex);
        BreatheOverlay overlay = new BreatheOverlay(skin, stage.getWidth(), stage.getHeight(),
            completed -> {
                game.analytics().event(AnalyticsEvents.BREATHE_DONE, AnalyticsEvents.P_LEVEL,
                    levelIndex, AnalyticsEvents.P_RESULT, completed ? "done" : "skipped");
                if (!disposed) {
                    game.goToLevel(levelIndex + 1);
                }
            });
        stage.addActor(overlay);
    }

    /** "1 more daily win" / "2 more daily wins", towards the streak palette. */
    private String remainingDailyWins() {
        int left = Math.max(1, GameConfig.DAILY_PALETTE_STREAK - game.save().dailyWinStreak);
        return left == 1 ? T.raw("1 more daily win") : T.rawf("{0} more daily wins", left);
    }

    private static final float STAR_SIZE = 68f;
    private static final float STAR_DELAY = 0.1f;
    private static final float STAR_STAGGER = 0.22f;
    private static final float STAR_DROP = 0.3f;

    /**
     * The result card: stars drop in one by one, a record badge pops, the coins fly up into the
     * HUD purse (which counts up as they land), and NEXT breathes to invite the next level.
     * Nothing here blocks input; the buttons work from the first frame.
     */
    private void showResult(WinSummary summary, int coinsEarned, int coinsBefore,
                            boolean paletteUnlockedNow) {
        boolean perfect = summary.perfect;
        // Three ways on, three different places - no two lead to the same screen:
        //   NEXT  -> campaign: next level / daily: back to the campaign level /
        //            challenge: the challenges list to pick another one;
        //   REPLAY -> this same board again;  HOME -> home;  SHARE keeps the card open.
        final Runnable goNext = () -> {
            if (dailyChallenge) {
                afterBreak(() -> game.goToLevel(game.save().currentLevel));
            } else if (challengeRun) {
                game.goToChallenges();
            } else {
                afterBreak(() -> game.goToLevel(levelIndex + 1));
            }
        };
        final Runnable goReplay = () -> {
            if (dailyChallenge) {
                game.goToDailyChallenge(LocalDate.parse(dailyDate));
            } else if (challengeRun) {
                game.goToChallenge(challengeRules); // replay it, not the list
            } else {
                afterBreak(() -> game.goToLevel(levelIndex));
            }
        };
        final Dialog[] resultDialog = new Dialog[1];
        Dialog dialog = Ui.dialog(skin, null, choice -> { }); // buttons wire themselves below
        resultDialog[0] = dialog;
        Table content = dialog.getContentTable();
        content.pad(26f, 30f, 22f, 30f);

        // --- header: what was played, then the big word, which pops in like the praise did
        String context = dailyChallenge ? T.r("DAILY CHALLENGE")
            : challengeRun ? T.r("CHALLENGE") : T.f("LEVEL {0}", LevelNumbers.format(levelIndex));
        Label contextLabel = new Label(context, skin, "accent");
        content.add(contextLabel).row();
        Label word = new Label(perfect ? T.r("Perfect!") : dailyChallenge ? T.r("Done!") : T.r("Cleared!"),
            skin, "display");
        if (perfect) word.setColor(Palette.GOLD);
        Container<Label> wordBox = new Container<>(word);
        wordBox.setTransform(true);
        wordBox.pack();
        wordBox.setOrigin(Align.center);
        wordBox.setScale(0.6f);
        wordBox.getColor().a = 0f;
        wordBox.addAction(Actions.parallel(
            Actions.fadeIn(0.15f),
            Actions.scaleTo(1f, 1f, 0.32f, Interpolation.swingOut)));
        content.add(wordBox).padTop(-6f).row();

        // --- stars: the earned ones drop in from above, one after another, each a note higher
        Table starsRow = new Table();
        float starsDone = STAR_DELAY + STAR_DROP;
        for (int i = 0; i < 3; i++) {
            Image star = new Image(skin.getDrawable("icon-star"));
            star.setSize(STAR_SIZE, STAR_SIZE);
            star.setOrigin(Align.center);
            boolean earnedStar = i < summary.stars;
            if (earnedStar) {
                final float pitch = 1f + 0.12f * i;
                star.setColor(Palette.GOLD);
                star.getColor().a = 0f;
                star.setScale(2.2f);
                star.addAction(Actions.sequence(
                    Actions.delay(STAR_DELAY + i * STAR_STAGGER),
                    Actions.run(() -> game.sound().play(SoundManager.Cue.DROP, pitch)),
                    Actions.parallel(
                        Actions.fadeIn(0.1f),
                        Actions.scaleTo(1f, 1f, STAR_DROP, Interpolation.swingOut))));
                starsDone = STAR_DELAY + i * STAR_STAGGER + STAR_DROP;
            } else {
                star.setColor(Palette.EDGE);
                star.getColor().a = 0.35f;
            }
            starsRow.add(star).size(STAR_SIZE).pad(0f, 6f, 0f, 6f).padTop(i == 1 ? 0f : 10f);
        }
        // A warm halo behind the stars, so the card has a centre of light, not just text.
        Image halo = new Image(skin.getDrawable("glow"));
        halo.setColor(Palette.GOLD.r, Palette.GOLD.g, Palette.GOLD.b, summary.stars > 0 ? 0.30f : 0.10f);
        com.badlogic.gdx.scenes.scene2d.ui.Stack starStack = new com.badlogic.gdx.scenes.scene2d.ui.Stack();
        starStack.add(halo);
        starStack.add(starsRow);
        content.add(starStack).size(340f, 150f).padTop(4f).row();

        String detail = moves == 1 ? T.raw("1 move") : T.rawf("{0} moves", moves);
        if (parMoves != Level.PAR_UNKNOWN) {
            detail = perfect ? T.rawf("{0}  •  can't be beaten", detail)
                : T.rawf("{0}  •  best possible {1}", detail, parMoves);
        }
        content.add(new Label(T.show(detail), skin, "dim")).padTop(10f).row();

        // --- record line (campaign only): a badge that pops once the stars have landed
        if (!dailyChallenge && !challengeRun) {
            if (summary.newRecord) {
                Label record = new Label(T.f("NEW RECORD!  was {0}", summary.previousBest), skin, "gold");
                Container<Label> badge = new Container<>(record);
                badge.setTransform(true);
                badge.pack();
                badge.setOrigin(Align.center);
                badge.setScale(0f);
                badge.addAction(Actions.sequence(
                    Actions.delay(starsDone + 0.05f),
                    Actions.run(() -> game.sound().play(SoundManager.Cue.TUBE)),
                    Actions.scaleTo(1.15f, 1.15f, 0.18f, Interpolation.pow2Out),
                    Actions.scaleTo(1f, 1f, 0.16f, Interpolation.swingOut)));
                content.add(badge).padTop(8f).row();
            } else if (!summary.firstClear) {
                content.add(new Label(T.f("Your best: {0} moves", summary.bestAfter), skin, "caption"))
                    .padTop(8f).row();
            }
        }

        // --- coins: the chip shows the award; the sprites fly from it to the purse
        Table rewardChip = null;
        if (dailyChallenge && coinsEarned == 0) {
            content.add(new Label(T.r("Daily reward already claimed"), skin, "caption"))
                .padTop(12f).row();
        } else if (challengeRun && coinsEarned == 0) {
            content.add(new Label(T.r("Challenge completed  •  no new achievement reward"),
                skin, "caption")).padTop(12f).row();
        } else {
            Label reward = new Label("+" + coinsEarned, skin, "gold");
            rewardChip = Ui.chip(skin, "icon-coin", Palette.GOLD, reward);
            content.add(rewardChip).padTop(12f).row();
            // Opt-in "double it" video: the best-performing rewarded placement in casual games,
            // offered only when a video is ready, granted once, after the SDK confirms.
            if (coinsEarned > 0 && game.ads().isRewardedReady()) {
                content.add(doubleCoinsButton(reward, coinsEarned)).padTop(10f).row();
            }
        }
        if (challengeRun && coinsEarned > 0) {
            content.add(new Label(T.r("Achievement reward"), skin, "caption")).padTop(8f).row();
        }
        if (dailyChallenge && coinsEarned > 0) {
            String streakMessage = paletteUnlockedNow
                ? T.r("3-day streak! New ball colours unlocked.")
                : game.save().dailyPaletteUnlocked ? T.r("Daily colour style unlocked.")
                : T.f("{0} to unlock colours.", remainingDailyWins());
            content.add(new Label(streakMessage, skin, "caption")).padTop(8f).row();
        }

        // --- star road: a skin unlocked by this level's stars, else how far the next one is
        if (!dailyChallenge && !challengeRun) {
            if (!starUnlocks.isEmpty()) {
                for (Cosmetic item : starUnlocks) {
                    Table unlocked = new Table();
                    unlocked.setBackground(skin.newDrawable("pill", Palette.GOLD));
                    unlocked.pad(8f, 16f, 8f, 18f);
                    boolean chapterReward = item.unlock() == Cosmetic.Unlock.CHAPTER;
                    unlocked.add(new Image(skin.newDrawable(chapterReward ? "icon-grid" : "icon-star",
                        Palette.ON_GOLD))).size(22f).padRight(8f);
                    Label text = new Label(T.f("New skin unlocked: {0}  •  {1}", Names.name(item.displayName()),
                        T.raw(item.category().title())), skin, "label");
                    text.setColor(Palette.ON_GOLD);
                    unlocked.add(text);
                    content.add(unlocked).padTop(10f).row();
                }
                game.sound().play(SoundManager.Cue.COIN);
            } else {
                StarRoad.Milestone next = StarRoad.next(game.save().totalStars());
                if (next != null) {
                    int missing = next.stars - game.save().totalStars();
                    Label road = new Label(T.f(missing == 1 ? T.k("{0} star to unlock {1} ({2})")
                            : T.k("{0} stars to unlock {1} ({2})"), missing, Names.name(next.reward.displayName()),
                        T.raw(next.reward.category().title()).toLowerCase(new java.util.Locale(T.language().code))),
                        skin, "caption");
                    road.setColor(Palette.GOLD);
                    content.add(road).padTop(8f).row();
                }
            }
        }

        // --- shop nudge: the first item the purse can now afford, announced once ever
        if (coinsEarned > 0) {
            java.util.List<Cosmetic> fresh = Shop.newlyAffordable(game.save());
            if (!fresh.isEmpty()) {
                Cosmetic item = fresh.get(0);
                game.save().markShopNotified(item.id());
                Label nudge = new Label(T.f("New in the shop: {0}  •  {1} coins", Names.name(item.displayName()),
                    item.price()), skin, "caption");
                nudge.setColor(Palette.GOLD);
                content.add(nudge).padTop(10f).row();
            }
        }

        // --- a breath every few levels: the calm the tagline promises, offered, never imposed
        if (!dailyChallenge && !challengeRun && summary.firstClear
            && Breathing.offersBreathAfter(levelIndex, GameConfig.BREATHE_EVERY_N_LEVELS)) {
            Table breath = new Table();
            breath.add(new Label(T.f("{0} levels in. Take a breath?", LevelNumbers.format(levelIndex)), skin, "caption"))
                .padRight(12f);
            breath.add(Ui.button(skin, T.f("BREATHE  •  {0} s", (int) Breathing.TOTAL_SECONDS),
                "ghost", () -> {
                    resultDialog[0].hide();
                    startBreath();
                })).height(48f);
            content.add(breath).padTop(14f).row();
        }

        // --- actions: one big way forward, then two labelled choices, then a quiet way home.
        // Every button says what it does in words; the glyph is only a hint.
        final float actionsWidth = 470f;
        content.add(Ui.divider(skin)).height(2f).width(actionsWidth).padTop(18f).row();
        // Daily: back to the campaign where the player left it; the level number is the home
        // screen's business, here it only reads like a jump to a random level.
        String primary = dailyChallenge ? T.r("CONTINUE") : challengeRun ? T.r("CHALLENGES") : T.r("NEXT LEVEL");
        com.badlogic.gdx.scenes.scene2d.ui.Button next = Ui.iconTextButton(skin, "icon-play",
            primary, "primary", goNext);
        content.add(next).width(actionsWidth).height(74f).padTop(18f).row();

        Table secondary = new Table();
        secondary.add(Ui.iconTextButton(skin, "icon-replay", T.r("REPLAY"), "ghost", goReplay))
            .width((actionsWidth - 12f) * 0.5f).height(60f).padRight(12f);
        secondary.add(Ui.iconTextButton(skin, "icon-share", T.r("SHARE"), "ghost",
            () -> shareResult(summary))) // the card stays; the share sheet opens over it
            .width((actionsWidth - 12f) * 0.5f).height(60f);
        content.add(secondary).padTop(12f).row();

        // Leaving a finished daily is its break, whichever way out the player takes.
        TextButton home = Ui.button(skin, T.r("HOME"), "ghost",
            dailyChallenge ? () -> afterBreak(game::goHome) : game::goHome);
        home.getLabel().setColor(Palette.TEXT_DIM);
        content.add(home).width(actionsWidth * 0.5f).height(52f).padTop(10f).row();

        dialog.getButtonTable().pad(0f); // no scene2d button row: the actions live in the card
        dialog.show(stage);

        // The actions arrive after the stars, so the eye reads the result first.
        for (com.badlogic.gdx.scenes.scene2d.Actor action
            : new com.badlogic.gdx.scenes.scene2d.Actor[] {next, secondary, home}) {
            action.getColor().a = 0f;
            action.addAction(Actions.delay(starsDone * 0.5f, Actions.fadeIn(0.25f, Interpolation.fade)));
        }
        // NEXT breathes gently once the card has settled: the eye lands on the way forward.
        next.setTransform(true);
        next.setOrigin(Align.center);
        next.addAction(Actions.sequence(
            Actions.delay(starsDone + 0.6f),
            Actions.forever(Actions.sequence(
                Actions.scaleTo(1.03f, 1.03f, 0.7f, Interpolation.sine),
                Actions.scaleTo(1f, 1f, 0.7f, Interpolation.sine)))));

        if (rewardChip != null && coinsEarned > 0) {
            final Table source = rewardChip;
            stage.addAction(Actions.delay(starsDone + 0.15f,
                Actions.run(() -> flyCoins(source, coinsEarned, coinsBefore))));
        }
    }

    /**
     * "WATCH • ×2" under the win reward: one rewarded video doubles the level's coins. The bonus
     * is credited once (button gone, flag set) and only from the SDK's reward callback, never on
     * a dismissed or failed video. It also counts as a full-screen ad for interstitial pacing.
     */
    private Actor doubleCoinsButton(Label rewardLabel, int coinsEarned) {
        final boolean[] doubled = {false};
        final Button[] holder = new Button[1];
        holder[0] = Ui.iconTextButton(skin, "icon-play", T.r("WATCH  •  ×2  COINS"), "gold", () -> {
            if (doubled[0] || holder[0].isDisabled()) {
                return;
            }
            holder[0].setDisabled(true);
            game.sound().play(SoundManager.Cue.BUTTON);
            game.pacing().onFullScreenAdShown();
            game.ads().showRewarded(new AdProvider.RewardListener() {
                @Override
                public void onRewarded(int coins) {
                    if (doubled[0] || disposed) {
                        return;
                    }
                    doubled[0] = true;
                    game.save().addCoins(coinsEarned);
                    rewardLabel.setText("+" + (coinsEarned * 2) + "  ×2");
                    hud.setCoins(game.save().coins);
                    holder[0].addAction(Actions.sequence(Actions.fadeOut(0.18f), Actions.visible(false)));
                    game.sound().play(SoundManager.Cue.COIN);
                    game.analytics().event(AnalyticsEvents.REWARDED_DOUBLE_COINS,
                        AnalyticsEvents.P_LEVEL, levelIndex, AnalyticsEvents.P_COINS, game.save().coins);
                }

                @Override
                public void onFailed() {
                    if (!disposed && !doubled[0]) {
                        holder[0].setDisabled(false);
                        Ui.toast(stage, skin, T.r("No video right now. Try again in a moment."), Palette.GOLD);
                    }
                }
            });
        });
        return holder[0];
    }

    /**
     * Sends a handful of coin sprites from {@code source} up to the HUD purse in staggered arcs;
     * each landing adds its share to the purse (which pops) and the last one lands exactly on
     * the saved total. The sprites sit above the dialog; the burst stays the last actor.
     */
    private void flyCoins(Table source, int coinsEarned, int coinsBefore) {
        if (disposed) {
            return;
        }
        int count = WinSummary.coinSprites(coinsEarned);
        Vector2 from = source.localToStageCoordinates(
            new Vector2(source.getWidth() * 0.5f, source.getHeight() * 0.5f));
        Vector2 to = hud.purseCenter(new Vector2());
        final float size = 34f;
        final int[] landed = {0};
        for (int k = 0; k < count; k++) {
            final int index = k;
            Image coin = new Image(skin.getDrawable("icon-coin"));
            coin.setColor(Palette.GOLD);
            coin.setSize(size, size);
            coin.setOrigin(Align.center);
            coin.setTouchable(Touchable.disabled);
            // Fan the take-off points a little so the coins don't leave as one blob.
            coin.setPosition(from.x - size * 0.5f + (k % 3 - 1) * 14f,
                from.y - size * 0.5f + (k % 2) * 10f);
            coin.setScale(0.6f);
            coin.getColor().a = 0f;
            stage.addActor(coin);
            coin.addAction(Actions.sequence(
                Actions.delay(k * 0.06f),
                Actions.parallel(Actions.fadeIn(0.08f),
                    Actions.scaleTo(1f, 1f, 0.12f, Interpolation.swingOut)),
                Actions.parallel(
                    ArcMoveAction.arcTo(to.x - size * 0.5f, to.y - size * 0.5f,
                        110f + k * 8f, 0.55f),
                    Actions.sequence(Actions.delay(0.4f),
                        Actions.scaleTo(0.45f, 0.45f, 0.15f, Interpolation.pow2In))),
                Actions.run(() -> {
                    landed[0] += WinSummary.coinsForSprite(coinsEarned, count, index);
                    boolean last = index == count - 1;
                    hud.setCoins(last ? game.save().coins
                        : Math.min(game.save().coins, coinsBefore + landed[0]));
                    if (index == 0) {
                        game.sound().play(SoundManager.Cue.COIN);
                    }
                }),
                Actions.removeActor()));
        }
        burst.toFront(); // the burst interrupts the batch, so it has to stay last
    }

    /**
     * A level is over and the player chose where to go next: the one natural break where an
     * interstitial is allowed, subject to {@link com.ben.laama.sortpuzzle.ads.AdPacing}. The
     * navigation always happens; the ad only delays it.
     */
    private void afterBreak(Runnable then) {
        // Onboarding is ad-free: the first levels decide whether the player comes back.
        boolean onboarding = game.save().completedCount() < GameConfig.INTERSTITIAL_MIN_CLEARED;
        // A finished daily always qualifies (interval permitting); campaign levels every Nth.
        boolean due = dailyChallenge ? game.pacing().onDailyDone() : game.pacing().onBreak();
        if (!onboarding && due && game.ads().isInterstitialReady()) {
            game.pacing().onFullScreenAdShown();
            game.ads().showInterstitial(then);
        } else {
            then.run();
        }
    }

    // ------------------------------------------------------------------- dialogs
    /**
     * The "how to play" card. Shared with the settings screen.
     *
     * @param onClose invoked when the card is dismissed; may be {@code null}
     */
    public static void showHowToPlay(Stage stage, Skin skin, Runnable onClose) {
        Dialog dialog = Ui.dialog(skin, T.r("How to play"), value -> {
            if (onClose != null) {
                onClose.run();
            }
        });
        Table content = dialog.getContentTable();
        String[] steps = {
            T.r("Tap a tube, or drag its top balls, to pick them up."),
            T.r("Tap another tube or drag-and-drop onto it. It must be empty, or show the same colour."),
            T.r("Fill every tube with a single colour to win. Undo is free, always."),
            T.r("You get one free hint per level. Watch the move limit; a rewarded continue is optional.")
        };
        for (int i = 0; i < steps.length; i++) {
            Table row = new Table();
            Label number = new Label(String.valueOf(i + 1), skin, "heading");
            number.setColor(Palette.PRIMARY);
            row.add(number).top().padRight(16f).width(26f);
            row.add(Ui.paragraph(skin, steps[i], "default", 380f)).width(380f).left();
            content.add(row).padTop(i == 0 ? 10f : 14f).left().row();
        }
        dialog.button(Ui.button(skin, T.r("GOT IT"), "primary", () -> { }), Boolean.TRUE);
        dialog.show(stage);
    }

    // ------------------------------------------------------------------ lifecycle
    @Override
    public void render(float delta) {
        if (pendingLevel != null) {
            // show() and resize() have both run by now: safe to deal the board.
            Level generated = pendingLevel;
            pendingLevel = null;
            onLevelReady(generated);
            if (hasPendingPar) {
                hasPendingPar = false;
                onParReady(pendingPar);
            }
        }
        // The rewarded ad usually finishes loading a few seconds after the screen is built, so
        // the hint caption follows its availability instead of being baked in once.
        boolean ready = game.ads().isRewardedReady();
        if (ready != lastRewardedReady) {
            lastRewardedReady = ready;
            hud.setHintCaption(hintCaption());
        }
        super.render(delta);
    }

    private boolean lastRewardedReady;
    /** Set on the first {@link #show()}; a level that arrives earlier waits in {@link #pendingLevel}. */
    private boolean shown;
    private Level pendingLevel;
    private int pendingPar;
    private boolean hasPendingPar;

    @Override
    public void show() {
        super.show();
        shown = true;
    }

    /**
     * The world size changes with the window (desktop) or with a fold/rotation on large screens
     * (Android 16 ignores the portrait lock on sw >= 600 dp), so the hand-placed board follows.
     */
    @Override
    protected void onResized(float worldWidth, float worldHeight) {
        hud.setLandscape(isLandscape());
        if (phase == Phase.LOADING || state == null || tubes.size == 0) {
            return;
        }
        deselect();
        relayout();
    }

    @Override
    protected void onBackPressed() {
        game.goHome();
    }

    /**
     * The world map: a campaign level is played in its chapter's biome, unless the player chose
     * a theme of their own in the shop. Applied by {@link BaseScreen#applyLook()} on show.
     */
    @Override
    protected Cosmetic themeOverride() {
        if (dailyChallenge || challengeRun || Shop.equipped(game.save(), Cosmetic.Category.THEME)
            != Cosmetic.defaultFor(Cosmetic.Category.THEME)) {
            return null;
        }
        return Cosmetic.byId(World.biomeFor(levelIndex).themeId);
    }

    @Override
    public void hide() {
        // No look restore here: the next screen applies its own in show(). Restoring here ran
        // after the next screen was built and swapped its theme (and backdrop) under it.
        super.hide();
    }

    @Override
    public void dispose() {
        disposed = true;
        burst.dispose();
        super.dispose();
    }
}
