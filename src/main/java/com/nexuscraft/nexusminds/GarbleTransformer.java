package com.nexuscraft.nexusminds;

import java.util.Random;

/**
 * Deterministically-random corruption of a plain-English config template into "monster speech" --
 * the same line reads differently every time it's spoken, but always legibly enough to recognize.
 * Purely cosmetic string transformation; carries no game logic.
 */
final class GarbleTransformer {

    private static final char[] GLITCH_CHARS = {'#', '%', '&', '*', '~', '^'};
    private static final double GLITCH_INSERT_CHANCE = 0.06;

    private final Random random = new Random();

    String garble(String template) {
        StringBuilder out = new StringBuilder(template.length() + 4);
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            out.append(substitute(c));
            if (random.nextDouble() < GLITCH_INSERT_CHANCE) {
                out.append(GLITCH_CHARS[random.nextInt(GLITCH_CHARS.length)]);
            }
        }
        return out.toString().toUpperCase();
    }

    private char substitute(char c) {
        return switch (Character.toLowerCase(c)) {
            case 'e' -> '3';
            case 'a' -> '4';
            case 'o' -> '0';
            case 'i' -> '1';
            case 's' -> '5';
            default -> c;
        };
    }
}
