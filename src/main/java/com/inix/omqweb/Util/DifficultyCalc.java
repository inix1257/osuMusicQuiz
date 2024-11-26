package com.inix.omqweb.Util;

import com.inix.omqweb.Game.GameDifficulty;

public class DifficultyCalc {
    public static double DIFF_EASY_BAR = 0.7;
    public static double DIFF_NORMAL_BAR = 0.4;
    public static double DIFF_HARD_BAR = 0.3;
    public static double DIFF_INSANE_BAR = 0.1;
    public static double DIFF_EXTRA_BAR = 0.0;

    public static GameDifficulty getDifficulty(double guessRate) {
        if (guessRate >= DIFF_EASY_BAR) {
            return GameDifficulty.EASY;
        } else if (guessRate >= DIFF_NORMAL_BAR) {
            return GameDifficulty.NORMAL;
        } else if (guessRate >= DIFF_HARD_BAR) {
            return GameDifficulty.HARD;
        } else if (guessRate >= DIFF_INSANE_BAR) {
            return GameDifficulty.INSANE;
        } else {
            return GameDifficulty.EXTRA;
        }
    }
}
