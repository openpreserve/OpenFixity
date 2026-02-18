package org.openpreservation.fixity.utils;

public final class OS {
    private static String OS = System.getProperty("os.name").toLowerCase();

    private OS() {
        // Prevent instantiation
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    public static boolean isMacOs() {
        return (OS.indexOf("mac os x") >= 0);
    }

    public static boolean isNix() {
        return (OS.indexOf("nix") >= 0
                || OS.indexOf("nux") >= 0
                || OS.indexOf("aix") > 0);
    }
}
