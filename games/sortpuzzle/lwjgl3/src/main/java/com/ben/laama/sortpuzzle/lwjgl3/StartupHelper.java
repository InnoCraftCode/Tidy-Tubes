package com.ben.laama.sortpuzzle.lwjgl3;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Relaunches the game in a fresh JVM when something can only be decided before the process
 * starts.
 *
 * <p>Two cases need it. macOS requires GLFW to run on the first thread of the process, which the
 * JVM only does when started with {@code -XstartOnFirstThread}, and nobody remembers to pass
 * that flag. And ANGLE has to have its EGL library resolved before GLFW is initialised — GLFW
 * initialises once per process, so a fallback to ANGLE after a failed native attempt is too
 * late in the same JVM.</p>
 */
final class StartupHelper {

    private static final String MAC_RESTART_MARKER = "sortpuzzle.jvmRestarted";

    /** System property naming the graphics backend a relaunched JVM must use, and only that one. */
    static final String BACKEND_PROPERTY = "sortpuzzle.gl";

    private StartupHelper() {
    }

    /**
     * @return {@code true} when a replacement JVM was started and the caller should return from
     *         {@code main} immediately without touching libGDX
     */
    static boolean startNewJvmIfRequired() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!osName.contains("mac")) {
            return false;
        }
        if (Boolean.parseBoolean(System.getProperty(MAC_RESTART_MARKER))) {
            return false; // this *is* the replacement JVM
        }
        long pid = ProcessHandle.current().pid();
        if ("1".equals(System.getenv("JAVA_STARTED_ON_FIRST_THREAD_" + pid))) {
            return false; // already launched correctly, nothing to do
        }
        return relaunch("-XstartOnFirstThread", "-D" + MAC_RESTART_MARKER + "=true");
    }

    /**
     * Restarts the game in a clean JVM pinned to one graphics backend.
     *
     * @param backend value for {@link #BACKEND_PROPERTY}
     * @return {@code true} when the replacement JVM ran
     */
    static boolean relaunchWithBackend(String backend) {
        return relaunch("-D" + BACKEND_PROPERTY + "=" + backend);
    }

    private static boolean relaunch(String... extraJvmArguments) {
        String javaBinary = System.getProperty("java.home")
            + File.separator + "bin" + File.separator + "java";

        List<String> command = new ArrayList<>();
        command.add(javaBinary);
        command.addAll(Arrays.asList(extraJvmArguments));
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Lwjgl3Launcher.class.getName());

        try {
            Process process = new ProcessBuilder(command).inheritIO().start();
            process.waitFor();
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return true;
        } catch (Exception failed) {
            System.err.println("[Ball Sort] Could not restart the JVM: " + failed);
            return false;
        }
    }
}
