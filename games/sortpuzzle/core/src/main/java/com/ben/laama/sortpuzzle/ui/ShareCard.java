package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.ben.laama.sortpuzzle.GameConfig;

import java.nio.ByteBuffer;

/**
 * Renders a result card to a PNG for the share sheet: the solved board in the player's
 * current look, the stars, the move count and an invitation. Drawn once into a frame buffer
 * with the game's own skin, so it looks exactly like the game and costs nothing to maintain.
 */
public final class ShareCard {

    /** 4:5, the portrait ratio social feeds show in full. */
    public static final int WIDTH = 1080;
    public static final int HEIGHT = 1350;
    private static final float WORLD_WIDTH = 720f;
    private static final float WORLD_HEIGHT = 900f;
    private static final String FILE = "share/tidy-tubes-result.png";

    private ShareCard() {
    }

    /** What the card says. */
    public static final class Result {
        public final String heading;      // "Daily puzzle  •  4 Nov" or "Level 24"
        public final int colours;
        public final int emptyTubes;
        public final int stars;
        public final int moves;
        public final int parMoves;        // 0 when unknown
        public final boolean perfect;

        public Result(String heading, int colours, int emptyTubes, int stars, int moves,
                      int parMoves, boolean perfect) {
            this.heading = heading;
            this.colours = colours;
            this.emptyTubes = emptyTubes;
            this.stars = stars;
            this.moves = moves;
            this.parMoves = parMoves;
            this.perfect = perfect;
        }
    }

    /**
     * Draws the card and writes it to {@code local/share/tidy-tubes-result.png}.
     *
     * @param restore the caller's viewport, re-applied once the frame buffer is released
     * @return the PNG
     */
    public static FileHandle render(Skin skin, TubeTextures tubes, Batch batch, Viewport restore,
                                    Result result) {
        FrameBuffer frame = new FrameBuffer(Pixmap.Format.RGB888, WIDTH, HEIGHT, false);
        Stage stage = new Stage(new FitViewport(WORLD_WIDTH, WORLD_HEIGHT), batch);
        try {
            stage.getViewport().update(WIDTH, HEIGHT, true);
            build(stage, skin, tubes, result);
            frame.begin();
            Gdx.gl.glClearColor(Palette.BG_BOTTOM.r, Palette.BG_BOTTOM.g, Palette.BG_BOTTOM.b, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            stage.act(0f);
            stage.draw();
            Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, WIDTH, HEIGHT);
            frame.end();
            flipVertically(pixmap);
            FileHandle file = Gdx.files.local(FILE);
            file.parent().mkdirs();
            PixmapIO.writePNG(file, pixmap);
            pixmap.dispose();
            return file;
        } finally {
            stage.dispose();
            frame.dispose();
            if (restore != null) {
                restore.apply();
            }
        }
    }

    private static void build(Stage stage, Skin skin, TubeTextures tubes, Result result) {
        // Backdrop: the themed gradient plus the two aurora washes, like every screen.
        Image gradient = new Image(skin.getDrawable("bg"));
        gradient.setBounds(0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
        stage.addActor(gradient);
        stage.addActor(aurora(skin, Palette.AURORA_A, -120f, 480f, 760f));
        stage.addActor(aurora(skin, Palette.AURORA_B, 260f, -140f, 700f));

        Table root = new Table();
        root.setFillParent(true);
        root.top().pad(54f, 40f, 44f, 40f);
        stage.addActor(root);

        root.add(new Label(GameConfig.APP_NAME, skin, "display")).row();
        Label heading = new Label(result.heading, skin, "dim");
        root.add(heading).padTop(2f).row();

        root.add(board(skin, tubes, result)).padTop(30f).row();

        Table stars = new Table();
        for (int i = 0; i < 3; i++) {
            Image star = new Image(skin.getDrawable("icon-star"));
            star.setColor(i < result.stars ? Palette.GOLD : Palette.EDGE);
            if (i >= result.stars) star.getColor().a = 0.35f;
            stars.add(star).size(64f).pad(0f, 6f, 0f, 6f).padTop(i == 1 ? 0f : 10f);
        }
        root.add(stars).padTop(26f).row();

        String detail = result.perfect ? "Perfect  •  " + result.moves + " moves"
            : result.moves + " moves"
                + (result.parMoves > 0 ? "  •  best possible " + result.parMoves : "");
        Label detailLabel = new Label(detail, skin, "heading");
        root.add(detailLabel).padTop(14f).row();

        root.add().expandY().row();
        Label invite = new Label("Can you beat it?", skin, "title");
        invite.setColor(Palette.PRIMARY);
        root.add(invite).row();
        root.add(new Label("Tidy Tubes  •  free on Google Play", skin, "dim")).padTop(4f).row();
    }

    /** The solved board: one full tube per colour, then the spare tubes, sized to fit. */
    private static Group board(Skin skin, TubeTextures tubes, Result result) {
        int count = result.colours + result.emptyTubes;
        int perRow = Math.min(count, 6);
        int rows = (count + perRow - 1) / perRow;
        float gap = 14f;
        float maxWidth = WORLD_WIDTH - 100f;
        float ballSize = Math.min(52f,
            (maxWidth - gap * (perRow - 1)) / perRow - TubeActor.INNER_PADDING * 2f);
        float tubeWidth = TubeActor.widthFor(ballSize);
        float tubeHeight = TubeActor.heightFor(ballSize);
        float rowGap = 34f;
        Group group = new Group();
        group.setSize(perRow * tubeWidth + (perRow - 1) * gap,
            rows * tubeHeight + (rows - 1) * rowGap);
        Group balls = new Group();
        for (int i = 0; i < count; i++) {
            int row = i / perRow;
            int column = i % perRow;
            int inThisRow = Math.min(perRow, count - row * perRow);
            float rowWidth = inThisRow * tubeWidth + (inThisRow - 1) * gap;
            float x = (group.getWidth() - rowWidth) * 0.5f + column * (tubeWidth + gap);
            float y = (rows - 1 - row) * (tubeHeight + rowGap);
            TubeActor tube = new TubeActor(skin, tubes, i, ballSize);
            tube.setSize(tubeWidth, tubeHeight);
            tube.setHome(x, y);
            group.addActor(tube);
            if (i < result.colours) {
                for (int slot = 0; slot < GameConfig.TUBE_CAPACITY; slot++) {
                    BallActor ball = new BallActor(skin, i, ballSize);
                    ball.setPosition(tube.ballX(), tube.ballY(slot));
                    balls.addActor(ball);
                }
            }
        }
        group.addActor(balls);
        return group;
    }

    private static Image aurora(Skin skin, Color color, float x, float y, float size) {
        Image glow = new Image(skin.getDrawable("glow"));
        glow.setColor(color.r, color.g, color.b, 0.22f);
        glow.setBounds(x, y, size, size);
        glow.setAlign(Align.center);
        return glow;
    }

    /** GL reads the frame buffer bottom-up; PNG wants top-down. */
    private static void flipVertically(Pixmap pixmap) {
        int width = pixmap.getWidth();
        int height = pixmap.getHeight();
        int rowBytes = width * 4; // RGBA8888 from getFrameBufferPixmap
        ByteBuffer pixels = pixmap.getPixels();
        byte[] top = new byte[rowBytes];
        byte[] bottom = new byte[rowBytes];
        for (int y = 0; y < height / 2; y++) {
            int topIndex = y * rowBytes;
            int bottomIndex = (height - 1 - y) * rowBytes;
            pixels.position(topIndex);
            pixels.get(top);
            pixels.position(bottomIndex);
            pixels.get(bottom);
            pixels.position(topIndex);
            pixels.put(bottom);
            pixels.position(bottomIndex);
            pixels.put(top);
        }
        pixels.position(0);
    }
}
