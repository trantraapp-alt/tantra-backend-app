package com.hyperlocal.tantra.constants;

public final class Constants {

    // Private constructor to prevent instantiation of this utility class
    private Constants() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Alpha-numeric pool excluding confusing characters like 0, O, 1, and I
     */
    public static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // 100% Letters
    public static final String DIGITS = "23456789";                  // 100% Numbers
 }