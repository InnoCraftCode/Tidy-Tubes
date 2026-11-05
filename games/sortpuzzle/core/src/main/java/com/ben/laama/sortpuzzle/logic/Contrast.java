package com.ben.laama.sortpuzzle.logic;

/**
 * WCAG 2 contrast arithmetic on hex colours, so the catalogue can be checked by a unit test:
 * text must stay readable on every theme, and the ten ball colours of a palette must stay
 * apart from each other and from the backdrop.
 */
public final class Contrast {

    private Contrast() {
    }

    /** Relative luminance (0 black .. 1 white) of an RGB or RGBA hex string; alpha ignored. */
    public static double luminance(String hex) {
        int rgb = Integer.parseUnsignedInt(hex.substring(0, 6), 16);
        double r = channel(((rgb >> 16) & 0xFF) / 255.0);
        double g = channel(((rgb >> 8) & 0xFF) / 255.0);
        double b = channel((rgb & 0xFF) / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    /** Contrast ratio between two colours, 1 (identical) .. 21 (black on white). */
    public static double ratio(String hexA, String hexB) {
        double a = luminance(hexA);
        double b = luminance(hexB);
        double light = Math.max(a, b);
        double dark = Math.min(a, b);
        return (light + 0.05) / (dark + 0.05);
    }

    /**
     * A cheap perceptual distance between two colours (0 identical), in RGB space weighted
     * towards what the eye notices. 60 or more reads as clearly different side by side.
     */
    public static double distance(String hexA, String hexB) {
        int a = Integer.parseUnsignedInt(hexA.substring(0, 6), 16);
        int b = Integer.parseUnsignedInt(hexB.substring(0, 6), 16);
        int dr = ((a >> 16) & 0xFF) - ((b >> 16) & 0xFF);
        int dg = ((a >> 8) & 0xFF) - ((b >> 8) & 0xFF);
        int db = (a & 0xFF) - (b & 0xFF);
        double meanRed = (((a >> 16) & 0xFF) + ((b >> 16) & 0xFF)) / 2.0;
        double weightRed = 2 + meanRed / 256.0;
        double weightBlue = 2 + (255 - meanRed) / 256.0;
        return Math.sqrt(weightRed * dr * dr + 4.0 * dg * dg + weightBlue * db * db);
    }

    private static double channel(double c) {
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
