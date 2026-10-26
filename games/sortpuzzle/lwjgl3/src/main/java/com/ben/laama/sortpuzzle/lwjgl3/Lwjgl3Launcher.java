package com.ben.laama.sortpuzzle.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.Main;

import java.util.Locale;

/**
 * Desktop development launcher. The shipping target is Android; this is for fast iteration.
 *
 * <h2>Getting a window at all</h2>
 * <p>An OpenGL context on Windows is not a given. A Remote Desktop session, a VM without 3D
 * acceleration, or a machine still on Microsoft's Basic Display Adapter all leave GLFW
 * reporting {@code GLFW_API_UNAVAILABLE: WGL: The driver does not appear to support OpenGL}.
 * So the launcher tries progressively less demanding configurations instead of assuming.</p>
 *
 * <p>The last rung, ANGLE, cannot be tried in the same process as the others. GLFW picks and
 * loads its client API library during {@code glfwInit}, which happens once per JVM — attempting
 * native OpenGL first and then switching to ANGLE only ever yields
 * {@code EGL: Library not found}. So ANGLE is reached by relaunching into a clean JVM pinned to
 * it with {@code -Dsortpuzzle.gl=ANGLE}, which is also how you can force it yourself.</p>
 */
public final class Lwjgl3Launcher {

    private static final String LOG = "[" + GameConfig.APP_NAME + "] ";

    /**
     * Ways to ask for a window, best first.
     *
     * <ul>
     *   <li>{@link #NATIVE} — the driver's own OpenGL, with multisampling. What you want.</li>
     *   <li>{@link #NATIVE_NO_MSAA} — same, minus the multisample buffer. Some drivers advertise
     *       OpenGL but have no pixel format matching a multisampled request.</li>
     *   <li>{@link #ANGLE} — OpenGL ES translated to Direct3D. Works where there is no usable
     *       OpenGL driver at all, because D3D falls back to a software rasteriser where GL just
     *       refuses. Needs its own JVM.</li>
     * </ul>
     */
    private enum Backend {
        NATIVE("native OpenGL"),
        NATIVE_NO_MSAA("native OpenGL without multisampling"),
        ANGLE("ANGLE (OpenGL ES over Direct3D)");

        private final String description;

        Backend(String description) {
            this.description = description;
        }
    }

    /** Tried in-process, in order. ANGLE is deliberately absent — see the class docs. */
    private static final Backend[] IN_PROCESS_LADDER = {Backend.NATIVE, Backend.NATIVE_NO_MSAA};

    private Lwjgl3Launcher() {
    }

    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) {
            return; // macOS needed a relaunch with -XstartOnFirstThread
        }

        String pinned = System.getProperty(StartupHelper.BACKEND_PROPERTY);
        if (pinned != null) {
            runPinned(pinned);
            return;
        }

        for (Backend backend : IN_PROCESS_LADDER) {
            try {
                new Lwjgl3Application(new Main(), configuration(backend));
                return;
            } catch (Throwable failure) {
                System.err.println(LOG + "No window from " + backend.description + ": "
                    + rootCauseMessage(failure));
            }
        }

        System.err.println(LOG + "Restarting in a clean JVM to try "
            + Backend.ANGLE.description + "...");
        if (StartupHelper.relaunchWithBackend(Backend.ANGLE.name())) {
            return;
        }
        reportNoBackend();
    }

    /** Single attempt, no fallback — this is a JVM that was relaunched for one specific backend. */
    private static void runPinned(String name) {
        Backend backend;
        try {
            backend = Backend.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            System.err.println(LOG + "Unknown value for -D" + StartupHelper.BACKEND_PROPERTY
                + ": " + name + ". Valid: NATIVE, NATIVE_NO_MSAA, ANGLE.");
            return;
        }
        try {
            new Lwjgl3Application(new Main(), configuration(backend));
        } catch (Throwable failure) {
            System.err.println(LOG + "No window from " + backend.description + ": "
                + rootCauseMessage(failure));
            reportNoBackend();
        }
    }

    private static void reportNoBackend() {
        System.err.println();
        System.err.println(LOG + "This machine could not give us a window through any "
            + "graphics backend. Usual causes, in order of likelihood:");
        System.err.println("  1. You are on a Remote Desktop session. Run the game at the "
            + "physical machine instead.");
        System.err.println("  2. The graphics driver is missing. Open dxdiag and look at the "
            + "Display tab: 'Microsoft Basic Display Adapter' means Windows never got the real "
            + "Intel/NVIDIA/AMD driver.");
        System.err.println("  3. You are in a VM with 3D acceleration turned off.");
        System.err.println();
        System.err.println(LOG + "The game itself is fine — 'gradlew :core:test' runs the "
            + "whole rule engine headless, and the Android build needs none of this.");
    }

    private static Lwjgl3ApplicationConfiguration configuration(Backend backend) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle(GameConfig.APP_NAME);

        // Half the virtual resolution: a phone-shaped window that fits on a laptop screen.
        int width = (int) (GameConfig.WORLD_WIDTH * 0.5f);
        int height = (int) (GameConfig.WORLD_HEIGHT * 0.5f);
        config.setWindowedMode(width, height);
        config.setWindowSizeLimits(280, 500, -1, -1);
        config.useVsync(true);
        config.setForegroundFPS(refreshRate() + 1);

        int samples = backend == Backend.NATIVE ? 2 : 0;
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, samples);

        if (backend == Backend.ANGLE) {
            config.setOpenGLEmulation(
                Lwjgl3ApplicationConfiguration.GLEmulation.ANGLE_GLES20, 0, 0);
        }
        return config;
    }

    /**
     * Matching the monitor's refresh rate avoids the classic "vsync on, still 300 fps" trap.
     * Querying it touches GLFW, which is exactly what may be broken here, so a failure falls
     * back to 60 rather than taking the process down.
     */
    private static int refreshRate() {
        try {
            return Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate;
        } catch (Throwable displayUnavailable) {
            return 60;
        }
    }

    private static String rootCauseMessage(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null ? cause.getClass().getSimpleName() : message;
    }
}
