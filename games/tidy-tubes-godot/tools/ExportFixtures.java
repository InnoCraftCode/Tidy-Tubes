import com.ben.laama.sortpuzzle.logic.Difficulty;
import com.ben.laama.sortpuzzle.logic.Level;
import com.ben.laama.sortpuzzle.logic.LevelGenerator;
import com.ben.laama.sortpuzzle.logic.Solver;
import com.ben.laama.sortpuzzle.model.BoardState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Writes the reference data the Godot port is tested against, straight from the libGDX game's
 * own classes: levels (board + seed + difficulty), solver results, and raw java.util.Random
 * output. Run after building the libGDX core:
 *
 * <pre>
 * java -cp ../sortpuzzle/core/build/classes/java/main tools/ExportFixtures.java tests/fixtures/levels.json 1 120
 * </pre>
 */
public final class ExportFixtures {

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        int first = Integer.parseInt(args[1]);
        int last = Integer.parseInt(args[2]);
        StringBuilder json = new StringBuilder("{\n");

        // java.util.Random reference streams.
        json.append("  \"random\": [\n");
        long[] seeds = {0L, 42L, -7L, LevelGenerator.seedFor(1), LevelGenerator.seedFor(105)};
        for (int s = 0; s < seeds.length; s++) {
            Random r = new Random(seeds[s]);
            json.append("    {\"seed\": \"").append(seeds[s]).append("\", \"ints\": [");
            int[] bounds = {2, 3, 7, 10, 48, 100, 1000, 1 << 20, 31, 5};
            for (int i = 0; i < 40; i++) {
                if (i > 0) json.append(", ");
                int bound = bounds[i % bounds.length];
                json.append("[").append(bound).append(", ").append(r.nextInt(bound)).append("]");
            }
            json.append("]}").append(s + 1 < seeds.length ? ",\n" : "\n");
        }
        json.append("  ],\n  \"levels\": [\n");

        long totalMs = 0;
        for (int level = first; level <= last; level++) {
            long t0 = System.nanoTime();
            Level generated = LevelGenerator.generate(level);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            totalMs += ms;
            BoardState board = generated.start();
            Solver.Result any = Solver.findAny(board, 200_000);
            json.append("    {\"level\": ").append(level)
                .append(", \"seed\": \"").append(LevelGenerator.seedFor(level)).append("\"")
                .append(", \"colors\": ").append(Difficulty.colorsFor(level))
                .append(", \"empty\": ").append(Difficulty.emptyTubesFor(level))
                .append(", \"depth\": ").append(Difficulty.shuffleDepthFor(level))
                .append(", \"ratio\": ").append(Difficulty.scrambleRatioFor(level))
                .append(", \"tubes\": [");
            int[] tubes = board.toEncodedArray();
            for (int i = 0; i < tubes.length; i++) {
                if (i > 0) json.append(", ");
                json.append(tubes[i]);
            }
            json.append("], \"anyMoves\": ").append(any.moveCount())
                .append(", \"anyNodes\": ").append(any.nodes())
                .append(", \"javaMs\": ").append(ms).append("}")
                .append(level < last ? ",\n" : "\n");
        }
        json.append("  ]\n}\n");
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.writeString(out, json);
        System.out.println("levels " + first + "-" + last + " generated in " + totalMs + " ms (JVM)");
    }
}
