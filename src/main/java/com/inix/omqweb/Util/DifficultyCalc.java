package com.inix.omqweb.Util;

import com.inix.omqweb.Game.GameDifficulty;

public class DifficultyCalc {
    public static double DIFF_EASY_BAR = 0.6;
    public static double DIFF_NORMAL_BAR = 0.35;
    public static double DIFF_HARD_BAR = 0.2;
    public static double DIFF_INSANE_BAR = 0.0;

    public static GameDifficulty getDifficulty(double guessRate) {
        if (guessRate >= DIFF_EASY_BAR) {
            return GameDifficulty.EASY;
        } else if (guessRate >= DIFF_NORMAL_BAR) {
            return GameDifficulty.NORMAL;
        } else if (guessRate >= DIFF_HARD_BAR) {
            return GameDifficulty.HARD;
        } else {
            return GameDifficulty.INSANE;
        }
    }
}
