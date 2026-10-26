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
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.Main;
import com.ben.laama.sortpuzzle.ads.AdProvider;
import com.ben.laama.sortpuzzle.audio.SoundManager;
import com.ben.laama.sortpuzzle.logic.Difficulty;
import com.ben.laama.sortpuzzle.logic.Hint;
import com.ben.laama.sortpuzzle.logic.HintEngine;
import com.ben.laama.sortpuzzle.logic.Level;
import com.ben.laama.sortpuzzle.logic.LevelGenerator;
import com.ben.laama.sortpuzzle.logic.MoveValidator;
import com.ben.laama.sortpuzzle.logic.RewardOffer;
import com.ben.laama.sortpuzzle.logic.Solver;
import com.ben.laama.sortpuzzle.logic.WinChecker;
import com.ben.laama.sortpuzzle.model.BoardState;
import com.ben.laama.sortpuzzle.model.Move;
import com.ben.laama.sortpuzzle.model.Packed;
import com.ben.laama.sortpuzzle.save.SaveData;
import com.ben.laama.sortpuzzle.ui.ArcMoveAction;
import com.ben.laama.sortpuzzle.ui.BallActor;
import com.ben.laama.sortpuzzle.ui.HudTable;
import com.ben.laama.sortpuzzle.ui.Palette;
import com.ben.laama.sortpuzzle.ui.TubeActor;
import com.ben.laama.sortpuzzle.ui.Ui;
import com.ben.laama.sortpuzzle.ui.WinBurst;

/**
 * The board itself: layout, input, animation, hints and the win/lose flow.
 *
 * <p>The model ({@link BoardState}) is updated the instant a tap is accepted; the actors then
 * animate towards wherever the model says they belong. Keeping those two apart is what makes
 * fast tapping feel right — a player can start the next move before the previous balls have
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
    private static final float TUBE_GAP = 16f;
    private static final float ROW_GAP = 70f;
    private static final float MAX_TUBE_WIDTH = 108f;
    private static final int MAX_TUBES_PER_ROW = 7;
    /** Two rows of seven. Past this the tubes get too narrow to tap reliably. */
    private static final int MAX_TUBES = 14;

    private final int levelIndex;

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
    private float ballSize = 64f;
    private boolean disposed;

    // ---- hints ----
    private int freeHints = GameConfig.FREE_HINTS_PER_LEVEL;
    private boolean hintsUnlocked;
    private boolean hintPending;
    private int hintRequestId;
    private int suggestedFrom = -1;
    private int suggestedTo = -1;
    private Actor hintGhost;

    public GameScreen(Main game, int levelIndex) {
        super(game);
        this.levelIndex = levelIndex;

        background.setIntensity(0.35f); // the board needs the attention, not the backdrop
        ballGroup.setTouchable(Touchable.disabled);
        stage.addActor(tubeGroup);
        stage.addActor(ballGroup);

        hud = new HudTable(skin, this);
        hud.setLevel(levelIndex, Difficulty.bandName(levelIndex) + "  •  "
            + Difficulty.colorsFor(levelIndex) + " colours");
        hud.setMoves(0, 0);
        hud.showPar(Level.PAR_UNKNOWN);
        hud.setCoins(game.save().coins);
        hud.setUndoEnabled(false);
        hud.setBoosters(availableBoosters());
        hud.setHintCaption(hintCaption());
        hud.setHintEnabled(false);
        stage.addActor(hud);

        loadingHolder = buildLoadingIndicator();
        stage.addActor(loadingHolder);

        stage.addActor(burst); // last, so its ShapeRenderer pass sits on top

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
        holder.add(new Label("Shuffling the tubes…", skin, "dim")).padTop(16f);
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
            Level generated = LevelGenerator.generate(levelIndex);
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
        level = generated;
        state = generated.start();
        phase = Phase.PLAYING;
        loadingHolder.remove();
        hud.setHintEnabled(true);
        buildBoard();

        if (!game.save().tutorialSeen && levelIndex == 1) {
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
        if (disposed || level == null) {
            return;
        }
        parMoves = par;
        level = level.withPar(par);
        moveLimit = Difficulty.moveLimitFor(par);
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
                BallActor ball = new BallActor(skin, Packed.slot(encoded, slot), ballSize);
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

    /** Positions one tube actor per model tube, in one or two centred rows. */
    private void layoutTubes() {
        int count = state.tubeCount();
        int rows = count <= 6 ? 1 : 2;
        int perRow = Math.min(MAX_TUBES_PER_ROW, (count + rows - 1) / rows);
        if (perRow * rows < count) {
            perRow = (count + rows - 1) / rows;
        }

        float worldWidth = stage.getWidth();
        float worldHeight = stage.getHeight();
        float available = Math.min(worldWidth, GameConfig.WORLD_WIDTH + 120f) - 2f * BOARD_MARGIN_X;
        float tubeWidth = Math.min(MAX_TUBE_WIDTH,
            (available - TUBE_GAP * (perRow - 1)) / perRow);
        ballSize = tubeWidth - 2f * TubeActor.INNER_PADDING;
        float tubeHeight = TubeActor.heightFor(ballSize);

        float stackHeight = rows * tubeHeight + (rows - 1) * ROW_GAP;
        // The HUD takes ~200 units at the top and ~150 at the bottom; centre the board between.
        float boardCenterY = 150f + (worldHeight - 150f - 220f) * 0.5f;
        float bottomY = boardCenterY - stackHeight * 0.5f;

        int placed = 0;
        for (int row = 0; row < rows; row++) {
            int inRow = Math.min(perRow, count - placed);
            float rowWidth = inRow * tubeWidth + TUBE_GAP * (inRow - 1);
            float rowX = (worldWidth - rowWidth) * 0.5f;
            // Row 0 is the top row, so it gets the higher y.
            float rowY = bottomY + (rows - 1 - row) * (tubeHeight + ROW_GAP);

            for (int column = 0; column < inRow; column++) {
                final int index = placed + column;
                TubeActor tube = new TubeActor(skin, game.tubes(), index, ballSize);
                tube.setSize(tubeWidth, tubeHeight);
                tube.setHome(rowX + column * (tubeWidth + TUBE_GAP), rowY);
                Ui.click(tube, () -> onTubeTapped(index));
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
                ball.setSize(ballSize, ballSize);
                ball.setOrigin(Align.center); // origin is derived from size, so it must follow
                ball.addAction(Actions.moveTo(tube.ballX(), tube.ballY(slot), 0.22f,
                    Interpolation.swingOut));
            }
        }
    }

    // -------------------------------------------------------------------- input
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
            game.sound().play(SoundManager.Cue.INVALID);
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
                // Held balls bob gently, slightly out of step, so the selection feels alive.
                float bob = GameConfig.BALL_LIFT_HEIGHT * 0.12f;
                ball.addAction(Actions.sequence(rise,
                    Actions.delay(0.04f * (size - 1 - slot)),
                    Actions.forever(Actions.sequence(
                        Actions.moveBy(0f, bob, 0.42f, Interpolation.sine),
                        Actions.moveBy(0f, -bob, 0.42f, Interpolation.sine)))));
            } else {
                ball.addAction(rise);
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
        }
        hud.setMoves(moves, moveLimit);
        hud.setUndoEnabled(stateHistory.size > 0);
        // The drop sound plays per ball as it lands - see transferBalls.

        boolean won = WinChecker.isWon(state);
        if (record && !won && Packed.isComplete(state.encodedAt(move.to()))) {
            final int completed = move.to();
            float landed = GameConfig.BALL_MOVE_DURATION
                + (move.count() - 1) * GameConfig.BALL_MOVE_STAGGER + 0.12f; // after the squash
            stage.addAction(Actions.delay(landed, Actions.run(() -> celebrateTube(completed))));
        }

        if (won) {
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

    /** Animates {@code count} balls from the top of one tube onto another. */
    private void transferBalls(int from, int to, int count) {
        Array<BallActor> source = stacks.get(from);
        Array<BallActor> target = stacks.get(to);
        TubeActor sourceTube = tubes.get(from);
        TubeActor targetTube = tubes.get(to);
        float clearance = Math.max(sourceTube.liftY(), targetTube.liftY());

        for (int i = 0; i < count; i++) {
            BallActor ball = source.pop();
            int slot = target.size;
            target.add(ball);

            float destinationY = targetTube.ballY(slot);
            float arc = Math.max(0f, clearance - Math.max(ball.getY(), destinationY)) + 18f;

            ball.clearActions();
            ball.toFront();
            ball.setScale(1f);
            // Each landing clinks a step higher than the last, so a run of balls plays a scale.
            final float pitch = 0.9f + 0.08f * slot;
            ball.addAction(Actions.sequence(
                Actions.delay(i * GameConfig.BALL_MOVE_STAGGER),
                ArcMoveAction.arcTo(targetTube.ballX(), destinationY, arc,
                    GameConfig.BALL_MOVE_DURATION),
                Actions.run(() -> game.sound().play(SoundManager.Cue.DROP, pitch)),
                Actions.scaleTo(1.16f, 0.84f, 0.05f, Interpolation.sineOut),
                Actions.scaleTo(1f, 1f, 0.18f, Interpolation.swingOut)));
        }
    }

    // ------------------------------------------------------------------ juice
    private static final String[] PRAISE = {"Nice!", "Great!", "Awesome!", "Amazing!"};
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

        int step = Math.min(tubesCompleted, PRAISE.length - 1);
        tubesCompleted++;
        popWord(PRAISE[step], tube.getX() + tube.getWidth() * 0.5f);
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
        float y = Math.min(boardTop + 12f, stage.getHeight() - 210f - box.getHeight() - 60f);
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
        if (phase != Phase.PLAYING || stateHistory.size == 0) {
            return;
        }
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
        hud.setUndoEnabled(stateHistory.size > 0);
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
        deselect();
        clearSuggestion();
        stateHistory.clear();
        moveHistory.clear();
        state = level.start();
        moves = 0;
        freeBoosters = GameConfig.FREE_BOOSTERS_PER_LEVEL;
        freeHints = GameConfig.FREE_HINTS_PER_LEVEL;
        phase = Phase.PLAYING;

        hud.setMoves(0, moveLimit);
        hud.setUndoEnabled(false);
        hud.setBoosters(availableBoosters());
        hud.setHintCaption(hintCaption());
        hud.setHintEnabled(true);
        hud.showPar(parMoves);
        buildBoard();
        game.sound().play(SoundManager.Cue.BUTTON);
    }

    @Override
    public void onBooster() {
        if (phase != Phase.PLAYING) {
            return;
        }
        if (state.tubeCount() >= MAX_TUBES) {
            toast("No room for another tube", Palette.DANGER);
            game.sound().play(SoundManager.Cue.INVALID);
            return;
        }
        SaveData save = game.save();
        if (freeBoosters > 0) {
            freeBoosters--;
        } else if (save.boosters > 0) {
            save.consumeBooster();
        } else if (save.coins >= GameConfig.BOOSTER_PRICE) {
            save.buyBooster();
            save.consumeBooster();
        } else {
            hud.setBoosters(0);
            game.sound().play(SoundManager.Cue.INVALID);
            toast("Not enough coins for another tube", Palette.DANGER);
            return;
        }

        deselect();
        state = state.withExtraTube();
        stacks.add(new Array<>(Packed.CAPACITY));
        relayout();

        hud.setBoosters(availableBoosters());
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

    private int availableBoosters() {
        return freeBoosters + game.save().boosters;
    }

    // --------------------------------------------------------------------- hints
    @Override
    public void onHint() {
        if (phase != Phase.PLAYING || hintPending) {
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
                            hud.setHintEnabled(true);
                            toast("Video closed early — no hint this time", Palette.TEXT_DIM);
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
                toast("Hints cost " + offer.price() + " coins — clear a level to earn more",
                    Palette.TEXT_DIM);
                break;
        }
    }

    private void confirmHintUnlock(int price) {
        Dialog dialog = Ui.dialog(skin, "Unlock hints?", choice -> {
            if (Boolean.TRUE.equals(choice) && !disposed && game.save().spendCoins(price)) {
                hud.setCoins(game.save().coins);
                game.sound().play(SoundManager.Cue.COIN);
                unlockHints();
            }
        });
        Table content = dialog.getContentTable();
        content.add(Ui.paragraph(skin, "Hints for the rest of this level, "
            + "for " + price + " coins.", "dim", 400f)).width(400f).padTop(6f).row();
        dialog.button(Ui.button(skin, "NOT NOW", "ghost", () -> { }), Boolean.FALSE);
        dialog.button(Ui.button(skin, "UNLOCK  •  " + price, "gold", () -> { }), Boolean.TRUE);
        dialog.show(stage);
    }

    private void unlockHints() {
        hintsUnlocked = true;
        hud.setHintCaption(hintCaption());
        hud.setHintEnabled(true);
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
                hud.showStatus("No way to win from here — undo", Palette.DANGER);
                if (animate) {
                    game.sound().play(SoundManager.Cue.INVALID);
                }
                break;
            case UNKNOWN:
                hud.showStatus("Too tangled to read from here", Palette.TEXT_DIM);
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

        BallActor ghost = new BallActor(skin, move.colorIndex(), ballSize);
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
        if (hintsUnlocked) {
            return "Hint ∞";
        }
        if (freeHints > 0) {
            return "Hint ×" + freeHints;
        }
        return game.ads().isRewardedReady() ? "Hint ▶" : "Hint • " + GameConfig.HINT_UNLOCK_PRICE;
    }

    // -------------------------------------------------------------- out of moves
    private boolean isOutOfMoves() {
        return moveLimit > 0 && moves >= moveLimit;
    }

    private void outOfMoves() {
        phase = Phase.OUT_OF_MOVES;
        deselect();
        clearSuggestion();
        hud.setHintEnabled(false);
        game.sound().play(SoundManager.Cue.INVALID);
        showOutOfMovesDialog();
    }

    private void showOutOfMovesDialog() {
        RewardOffer offer = RewardOffer.decide(game.ads().isRewardedReady(),
            game.save().coins, GameConfig.EXTRA_MOVES_PRICE);

        Dialog dialog = Ui.dialog(skin, "Out of moves", choice -> {
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
        content.add(new Label("You used all " + moveLimit + " of them.", skin, "dim")).padTop(4f).row();
        if (!offer.isAvailable()) {
            content.add(Ui.paragraph(skin, "+" + GameConfig.EXTRA_MOVES_PER_AD + " moves cost "
                + offer.price() + " coins. Clear a level to earn more.", "caption", 400f))
                .width(400f).padTop(10f).row();
        }

        dialog.button(Ui.button(skin, "HOME", "ghost", () -> { }), "home");
        dialog.button(Ui.button(skin, "RETRY", "secondary", () -> { }), "restart");
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
                    toast("Video closed early — no extra moves", Palette.TEXT_DIM);
                    showOutOfMovesDialog();
                }
            }
        });
    }

    private void grantExtraMoves() {
        moveLimit += GameConfig.EXTRA_MOVES_PER_AD;
        phase = Phase.PLAYING;
        hud.setMoves(moves, moveLimit);
        hud.setHintEnabled(true);
        toast("+" + GameConfig.EXTRA_MOVES_PER_AD + " moves", Palette.SUCCESS);
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
        popWord(perfect ? "Perfect!" : "Complete!", stage.getWidth() * 0.5f);
        SoundManager.Cue voice = perfect ? SoundManager.Cue.VOICE_PERFECT
            : SoundManager.Cue.VOICE_LEVEL_COMPLETE;
        game.sound().play(voice);

        int earned = game.save().recordWin(levelIndex, moves, parMoves);
        hud.setCoins(game.save().coins);
        stage.addAction(Actions.delay(0.9f, Actions.run(() -> {
            if (!disposed) {
                showResult(earned, perfect);
            }
        })));
    }

    private void showResult(int coinsEarned, boolean perfect) {
        Dialog dialog = Ui.dialog(skin, perfect ? "Perfect!" : "Level " + levelIndex + " cleared",
            choice -> {
                if ("next".equals(choice)) {
                    afterBreak(() -> game.goToLevel(levelIndex + 1));
                } else if ("replay".equals(choice)) {
                    afterBreak(() -> game.goToLevel(levelIndex));
                } else {
                    game.goHome();
                }
            });
        Table content = dialog.getContentTable();

        String detail = moves == 1 ? "1 move" : moves + " moves";
        if (parMoves != Level.PAR_UNKNOWN) {
            detail += perfect ? "  •  best possible" : "  •  best possible " + parMoves;
        }
        content.add(new Label(detail, skin, "dim")).padTop(4f).row();

        Label reward = new Label("+" + coinsEarned, skin, "gold");
        content.add(Ui.chip(skin, "icon-coin", Palette.GOLD, reward)).padTop(16f).row();

        dialog.button(Ui.iconButton(skin, "icon-home", () -> { }), "home");
        dialog.button(Ui.button(skin, "REPLAY", "ghost", () -> { }), "replay");
        dialog.button(Ui.button(skin, "NEXT", "primary", () -> { }), "next");
        dialog.show(stage);
    }

    /**
     * A level is over and the player chose where to go next: the one natural break where an
     * interstitial is allowed, subject to {@link com.ben.laama.sortpuzzle.ads.AdPacing}. The
     * navigation always happens; the ad only delays it.
     */
    private void afterBreak(Runnable then) {
        if (game.pacing().onBreak() && game.ads().isInterstitialReady()) {
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
        Dialog dialog = Ui.dialog(skin, "How to play", value -> {
            if (onClose != null) {
                onClose.run();
            }
        });
        Table content = dialog.getContentTable();
        String[] steps = {
            "Tap a tube to lift the balls on top.",
            "Tap another tube to drop them there. It must be empty, or show the same colour.",
            "Fill every tube with a single colour to win. Undo is free, always."
        };
        for (int i = 0; i < steps.length; i++) {
            Table row = new Table();
            Label number = new Label(String.valueOf(i + 1), skin, "heading");
            number.setColor(Palette.PRIMARY);
            row.add(number).top().padRight(16f).width(26f);
            row.add(Ui.paragraph(skin, steps[i], "default", 380f)).width(380f).left();
            content.add(row).padTop(i == 0 ? 10f : 14f).left().row();
        }
        dialog.button(Ui.button(skin, "GOT IT", "primary", () -> { }), Boolean.TRUE);
        dialog.show(stage);
    }

    // ------------------------------------------------------------------ lifecycle
    @Override
    public void render(float delta) {
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

    /**
     * The world size changes with the window (desktop) or with a fold/rotation on large screens
     * (Android 16 ignores the portrait lock on sw >= 600 dp), so the hand-placed board follows.
     */
    @Override
    protected void onResized(float worldWidth, float worldHeight) {
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

    @Override
    public void dispose() {
        disposed = true;
        burst.dispose();
        super.dispose();
    }
}
